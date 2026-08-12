package com.mongxin.livestart.pay.mq;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayResultProducer {
    public static final String TOPIC = "livestart_pay_pay-success_topic";
    private final RocketMQTemplate rocketMQTemplate;

    public SendResult send(PayResultEvent event) {
        return rocketMQTemplate.syncSend(
                TOPIC,
                MessageBuilder.withPayload(JSON.toJSONString(event))
                        .setHeader("KEYS", event.getOrderNo())
                        .build(),
                3000L
        );
    }
}
