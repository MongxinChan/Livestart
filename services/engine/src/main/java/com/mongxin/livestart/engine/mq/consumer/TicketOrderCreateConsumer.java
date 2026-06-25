package com.mongxin.livestart.engine.mq.consumer;

import cn.hutool.core.lang.Singleton;
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
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsume;
import com.mongxin.livestart.framework.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 票单异步下单落库消费者（高并发削峰核心组件）
 * <p>
 * 消费 RocketMQ 中的下单消息，在后台线程池中平滑并发地将订单数据写入 MySQL，
 * 写入成功后开启延时关单，失败则自动回滚并补偿 Redis 缓存库存。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "livestart.engine.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_engine_order-create_topic",
        consumerGroup = "livestart_engine_order-create_cg"
)
@RequiredArgsConstructor
public class TicketOrderCreateConsumer implements RocketMQListener<String> {

    private static final String STOCK_ROLLBACK_LUA_PATH = "lua/stock_rollback.lua";
    private static final int MAX_DB_STOCK_RETRY_TIMES = 3;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final MerchantAdminRemoteService merchantAdminRemoteService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderDelayCloseProducer orderDelayCloseProducer;

    /** 订单超时关单延时（15分钟，单位 ms） */
    private static final long ORDER_CLOSE_DELAY_MS = 15 * 60 * 1000L;

    @Override
    @NoMQDuplicateConsume(keyPrefix = "engine:idempotent:mq:create-order:", key = "#message")
    public void onMessage(String message) {
        log.info("[异步建单] 收到消息，message={}", message);

        MessageWrapper<TicketOrderCreateEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<TicketOrderCreateEvent>>() {});
        TicketOrderCreateEvent event = wrapper.getMessage();

        // 1. 查询最新的票种 SKU 信息
        TicketSkuDO sku = loadTicketSku(event.getSkuId());
        if (sku == null) {
            log.error("[异步建单] 票档不存在，开始回滚 Redis 预扣，skuId={}", event.getSkuId());
            rollbackPreDeductStock(event);
            return;
        }

        try {
            // 2. 编程式事务处理：乐观锁扣 DB 库存 + 写入订单 + 写入明细
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    TicketSkuDO latestSku = decrementStockWithRetry(event.getSkuId(), event.getCount());

                    BigDecimal totalAmount = latestSku.getSellingPrice().multiply(BigDecimal.valueOf(event.getCount()));
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
                        items.add(OrderItemDO.builder()
                                .orderNo(event.getOrderNo())
                                .userId(event.getUserId())
                                .visitorId(visitorId)
                                .eventId(latestSku.getEventId())
                                .skuId(latestSku.getId())
                                .checkCode(generateCheckCode())
                                .isChecked(0)
                                .build());
                    }
                    items.forEach(orderItemMapper::insert);
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    throw ex;
                }
            });

            // 3. 事务成功落库后，发送延时关单消息（15分钟后未支付自动关单）
            sendDelayCloseMessage(event);
        } catch (Exception ex) {
            log.error("[异步建单] 落库失败，开始回滚 Redis 预扣，orderNo={}", event.getOrderNo(), ex);
            rollbackPreDeductStock(event);
        }
    }

    private void rollbackPreDeductStock(TicketOrderCreateEvent event) {
        try {
            String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, event.getSkuId());
            String userLimitKey = String.format(
                    EngineRedisConstant.USER_TICKET_LIMIT_KEY,
                    event.getUserId(),
                    event.getEventId()
            );
            stringRedisTemplate.execute(
                    loadLongRedisScript(STOCK_ROLLBACK_LUA_PATH),
                    List.of(stockKey, userLimitKey),
                    String.valueOf(event.getCount()),
                    String.valueOf(event.getCount())
            );
            log.info("[异步建单] Redis 库存与限购计数已回滚，orderNo={}", event.getOrderNo());
        } catch (Exception ex) {
            log.error("[异步建单] Redis 回滚失败，orderNo={}", event.getOrderNo(), ex);
        }
    }

    private void sendDelayCloseMessage(TicketOrderCreateEvent event) {
        long closeTime = System.currentTimeMillis() + ORDER_CLOSE_DELAY_MS;
        OrderDelayCloseEvent closeEvent = OrderDelayCloseEvent.builder()
                .orderNo(event.getOrderNo())
                .userId(event.getUserId())
                .skuId(event.getSkuId())
                .eventId(event.getEventId())
                .count(event.getCount())
                .delayTime(closeTime)
                .build();
        try {
            SendResult sendResult = orderDelayCloseProducer.sendMessage(closeEvent);
            if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
                log.warn("[异步建单] 延时关单消息发送状态异常，orderNo={}", event.getOrderNo());
            }
        } catch (Exception ex) {
            log.error("[异步建单] 延时关单消息发送失败，orderNo={}", event.getOrderNo(), ex);
        }
    }

    private String generateCheckCode() {
        return UUID.fastUUID().toString(true).toUpperCase();
    }

    private TicketSkuDO decrementStockWithRetry(Long skuId, int count) {
        TicketSkuDO latestSku = null;
        for (int attempt = 0; attempt < MAX_DB_STOCK_RETRY_TIMES; attempt++) {
            latestSku = loadTicketSku(skuId);
            if (latestSku == null) {
                throw new ServiceException("票档不存在");
            }
            int decremented = ticketSkuMapper.decrementStock(
                    latestSku.getId(),
                    count,
                    latestSku.getVersion()
            );
            if (SqlHelper.retBool(decremented)) {
                return latestSku;
            }
        }
        throw new ServiceException("并发库存扣减失败，请重试");
    }

    private TicketSkuDO loadTicketSku(Long skuId) {
        Result<MerchantTicketSkuDetailRespDTO> result = merchantAdminRemoteService.getTicketSku(skuId);
        if (result == null || result.isFail() || result.getData() == null) {
            log.warn("[票种查询] MQ 落库前远程查询票种失败 | skuId={} | result={}", skuId, result);
            return null;
        }
        MerchantTicketSkuDetailRespDTO data = result.getData();
        TicketSkuDO sku = new TicketSkuDO();
        sku.setId(data.getId());
        sku.setEventId(data.getEventId());
        sku.setTitle(data.getTitle());
        sku.setOriginalPrice(data.getOriginalPrice());
        sku.setSellingPrice(data.getSellingPrice());
        sku.setTotalStock(data.getTotalStock());
        sku.setRemainingStock(data.getRemainingStock());
        sku.setLimitNum(data.getLimitNum());
        sku.setVersion(data.getVersion());
        return sku;
    }

    private DefaultRedisScript<Long> loadLongRedisScript(String classpath) {
        return Singleton.get(classpath, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(classpath)));
            script.setResultType(Long.class);
            return script;
        });
    }
}
