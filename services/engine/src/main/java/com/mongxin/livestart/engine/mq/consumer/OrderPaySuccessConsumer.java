package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 支付成功（出票）消费者
 * <p>
 * 消费支付成功消息，执行出票后置处理（如：发送短信通知、刷新用户订单缓存等）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "livestart.engine.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_engine_order-pay-success_topic",
        consumerGroup = "livestart_engine_order-pay-success_cg"
)
@RequiredArgsConstructor
public class OrderPaySuccessConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("[消费者] 支付成功出票消息：{}", message);

        MessageWrapper<OrderPaySuccessEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<OrderPaySuccessEvent>>() {});
        OrderPaySuccessEvent event = wrapper.getMessage();

        // TODO: 后续扩展：发送短信/App推送、刷新用户订单缓存等
        log.info("[消费者] 订单出票处理完成，orderNo={}，userId={}", event.getOrderNo(), event.getUserId());
    }
}
