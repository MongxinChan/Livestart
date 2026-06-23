package com.mongxin.livestart.engine.mq.consumer;

import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsume;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单超时关单消费者
 * <p>
 * 消费延时消息，将待支付的超时订单关闭，并归还 Redis 库存
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "livestart.engine.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_engine_order-delay-close_topic",
        consumerGroup = "livestart_engine_order-delay-close_cg"
)
@RequiredArgsConstructor
public class OrderDelayCloseConsumer implements RocketMQListener<String> {

    private final OrderMapper orderMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Override
    @NoMQDuplicateConsume(keyPrefix = "engine:idempotent:mq:delay-close:", key = "#message")
    public void onMessage(String message) {
        log.info("[消费者] 订单超时关单消息：{}", message);

        MessageWrapper<OrderDelayCloseEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<OrderDelayCloseEvent>>() {});
        OrderDelayCloseEvent event = wrapper.getMessage();

        // 查询订单（按 orderNo 查找，携带 userId 走分片路由）
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, event.getOrderNo())
                .eq(OrderDO::getUserId, event.getUserId());
        OrderDO order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            log.warn("[消费者] 关单消息中订单不存在，orderNo={}", event.getOrderNo());
            return;
        }

        // 只有待支付状态才关闭
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.info("[消费者] 订单状态非待支付，无需关单，orderNo={}，status={}", event.getOrderNo(), order.getStatus());
            return;
        }

        // CAS 更新订单状态为已取消
        int affected = orderMapper.updateOrderStatus(
                order.getId(),
                event.getUserId(),
                OrderStatusEnum.CANCELLED.getCode(),
                OrderStatusEnum.PENDING_PAYMENT.getCode()
        );

        if (affected > 0) {
            // 归还 Redis 库存
            try {
                String stockKey = String.format(
                        com.mongxin.livestart.engine.common.constant.EngineRedisConstant.TICKET_STOCK_KEY,
                        event.getSkuId());
                stringRedisTemplate.opsForValue().increment(stockKey, event.getCount());
                log.info("[消费者] 订单超时关单成功，已归还Redis库存，orderNo={}，skuId={}，count={}",
                        event.getOrderNo(), event.getSkuId(), event.getCount());
            } catch (Exception e) {
                log.error("[消费者] 归还Redis库存失败（非阻塞），orderNo={}", event.getOrderNo(), e);
            }
            // 归还 DB 库存
            ticketSkuMapper.returnStock(event.getSkuId(), event.getCount());
        } else {
            log.warn("[消费者] 关单 CAS 失败（订单状态已变更），orderNo={}", event.getOrderNo());
        }
    }
}
