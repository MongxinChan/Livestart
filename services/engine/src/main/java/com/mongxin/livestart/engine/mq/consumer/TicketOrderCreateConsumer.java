package com.mongxin.livestart.engine.mq.consumer;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderDelayCloseProducer;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 购票异步下单落库消费者（高并发削峰核心组件）
 * <p>
 * 消费 RocketMQ 中的下单消息，在后台线程池中平滑并发地将订单数据写入 MySQL 数据库，
 * 写入成功后开启延时关单，失败则自动回滚并补偿 Redis 缓存库存。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "livestart_engine_order-create_topic",
        consumerGroup = "livestart_engine_order-create_cg"
)
@RequiredArgsConstructor
public class TicketOrderCreateConsumer implements RocketMQListener<String> {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderDelayCloseProducer orderDelayCloseProducer;

    /** 订单超时关单延时（15分钟，单位 ms） */
    private static final long ORDER_CLOSE_DELAY_MS = 15 * 60 * 1000L;

    @Override
    public void onMessage(String message) {
        log.info("[消费者] 收到异步下单落库消息：{}", message);

        MessageWrapper<TicketOrderCreateEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<TicketOrderCreateEvent>>() {});
        TicketOrderCreateEvent event = wrapper.getMessage();

        // 1. 查询最新的票种 SKU 信息
        TicketSkuDO sku = ticketSkuMapper.selectById(event.getSkuId());
        if (sku == null) {
            log.error("[消费者] 异步下单失败：票种 SKU 不存在，skuId={}", event.getSkuId());
            compensateRedisStock(event);
            return;
        }

        try {
            // 2. 编程式事务处理：乐观锁扣 DB 库存 + 写入订单 + 写入明细
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    // DB 二次校验扣减库存（乐观锁）
                    int decremented = ticketSkuMapper.decrementStock(sku.getId(), event.getCount(), sku.getVersion());
                    if (!SqlHelper.retBool(decremented)) {
                        throw new ServiceException("库存不足，落库失败");
                    }

                    // 写订单主表
                    BigDecimal totalAmount = sku.getSellingPrice().multiply(BigDecimal.valueOf(event.getCount()));
                    Date now = new Date();
                    OrderDO order = OrderDO.builder()
                            .orderNo(event.getOrderNo())
                            .userId(event.getUserId())
                            .totalAmount(totalAmount)
                            .status(OrderStatusEnum.PENDING_PAYMENT.getCode())
                            .createTime(now)
                            .build();
                    orderMapper.insert(order);

                    // 写订单明细（每张票一条记录）
                    List<OrderItemDO> items = new ArrayList<>();
                    for (Long visitorId : event.getVisitorIds()) {
                        OrderItemDO item = OrderItemDO.builder()
                                .orderNo(event.getOrderNo())
                                .userId(event.getUserId())
                                .visitorId(visitorId)
                                .eventId(sku.getEventId())
                                .skuId(sku.getId())
                                .checkCode(generateCheckCode())
                                .isChecked(0)
                                .build();
                        items.add(item);
                    }
                    items.forEach(orderItemMapper::insert);

                    log.info("[消费者] 异步下单数据库事务执行成功，orderNo={}", event.getOrderNo());
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    log.error("[消费者] 异步下单本地事务执行异常，触发数据库回滚，orderNo={}", event.getOrderNo(), ex);
                    throw ex;
                }
            });

            // 3. 事务成功落库后，发送延时关单消息（15分钟后未支付自动关单）
            sendDelayCloseMessage(event);

        } catch (Exception e) {
            // 4. 出现异常进行 Redis 缓存库存回退补偿
            log.error("[消费者] 异步下单处理失败，开始执行 Redis 库存补偿，orderNo={}", event.getOrderNo());
            compensateRedisStock(event);
        }
    }

    /**
     * 回退并补偿 Redis 库存
     */
    private void compensateRedisStock(TicketOrderCreateEvent event) {
        try {
            String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, event.getSkuId());
            stringRedisTemplate.opsForValue().increment(stockKey, event.getCount());
            log.info("[消费者] Redis 库存补偿成功，skuId={}，回退张数={}", event.getSkuId(), event.getCount());
        } catch (Exception ex) {
            log.error("[消费者] Redis 库存补偿异常（非阻塞），skuId={}，回退张数={}", event.getSkuId(), event.getCount(), ex);
        }
    }

    /**
     * 发送 15 分钟延时关单消息
     */
    private void sendDelayCloseMessage(TicketOrderCreateEvent event) {
        long closeTime = System.currentTimeMillis() + ORDER_CLOSE_DELAY_MS;
        OrderDelayCloseEvent closeEvent = OrderDelayCloseEvent.builder()
                .orderNo(event.getOrderNo())
                .userId(event.getUserId())
                .skuId(event.getSkuId())
                .count(event.getCount())
                .delayTime(closeTime)
                .build();
        try {
            SendResult sendResult = orderDelayCloseProducer.sendMessage(closeEvent);
            if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
                log.warn("[消费者] 延时关单消息发送异常，返回值状态非 SEND_OK，orderNo={}", event.getOrderNo());
            }
        } catch (Exception ex) {
            log.error("[消费者] 延时关单消息发送失败，orderNo={}", event.getOrderNo(), ex);
        }
    }

    /**
     * 生成唯一核销码
     */
    private String generateCheckCode() {
        return UUID.fastUUID().toString(true).toUpperCase();
    }
}
