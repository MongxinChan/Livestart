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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(String message) {
        log.info("[消费者] 支付成功出票消息：{}", message);

        MessageWrapper<OrderPaySuccessEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<OrderPaySuccessEvent>>() {});
        OrderPaySuccessEvent event = wrapper.getMessage();

        String notifyKey = "engine:order:pay-success:notification:" + event.getOrderNo();
        Boolean firstDelivery = stringRedisTemplate.opsForValue()
                .setIfAbsent(notifyKey, "1", 30, TimeUnit.DAYS);
        if (!Boolean.TRUE.equals(firstDelivery)) {
            log.info("[消费者] 支付成功后置通知已处理，跳过重复消息，orderNo={}", event.getOrderNo());
            return;
        }

        // 毕业设计阶段使用本地模拟通知，替换为真实短信/App 推送实现时无需改变消费幂等逻辑。
        log.info("[模拟短信通知] 订单支付成功，orderNo={}，userId={}，tradeNo={}",
                event.getOrderNo(), event.getUserId(), event.getTradeNo());
        log.info("[消费者] 订单出票后置处理完成，orderNo={}，userId={}", event.getOrderNo(), event.getUserId());
    }
}
