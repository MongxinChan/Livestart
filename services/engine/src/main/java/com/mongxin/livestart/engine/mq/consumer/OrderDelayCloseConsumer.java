package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import com.mongxin.livestart.engine.service.StockRestoreService;
import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsume;
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
    private final StockRestoreService stockRestoreService;

    @Override
    @NoMQDuplicateConsume(keyPrefix = "engine:idempotent:mq:delay-close:", key = "#message")
    public void onMessage(String message) {
        log.info("[延时关单] 收到消息，message={}", message);

        MessageWrapper<OrderDelayCloseEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<OrderDelayCloseEvent>>() {});
        OrderDelayCloseEvent event = wrapper.getMessage();

        // 查询订单（按 orderNo 查找，携带 userId 走分片路由）
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, event.getOrderNo())
                .eq(OrderDO::getUserId, event.getUserId());
        OrderDO order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            log.warn("[延时关单] 订单不存在，orderNo={}", event.getOrderNo());
            return;
        }

        // 只有待支付状态才关闭
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.info("[延时关单] 订单状态已变更，跳过关单，orderNo={}, status={}", event.getOrderNo(), order.getStatus());
            return;
        }

        try {
            stockRestoreService.closeTimeoutOrder(
                    order.getId(), event.getUserId(), event.getOrderNo(),
                    event.getEventId(), event.getSkuId(), event.getCount());
        } catch (Exception ex) {
            log.error("[延时关单] 创建库存补偿任务失败，orderNo={}", event.getOrderNo(), ex);
            // 不能吞掉异常，否则幂等切面会把消息标记为已消费，后续只能依赖兜底扫描。
            throw new IllegalStateException("库存补偿任务创建失败，等待消息重试", ex);
        }
    }
}
