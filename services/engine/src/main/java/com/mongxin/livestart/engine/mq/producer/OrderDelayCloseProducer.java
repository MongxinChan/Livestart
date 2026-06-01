package com.mongxin.livestart.engine.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.mq.base.BaseSendExtendDTO;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * 订单超时关单延时消息生产者
 */
@Component
public class OrderDelayCloseProducer extends AbstractCommonSendProduceTemplate<OrderDelayCloseEvent> {

    private static final String TOPIC = "livestart_engine_order-delay-close_topic";
    private static final String EVENT_NAME = "订单超时关单";
    private static final long DEFAULT_TIMEOUT_MS = 3000L;

    public OrderDelayCloseProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(OrderDelayCloseEvent event) {
        return BaseSendExtendDTO.builder()
                .eventName(EVENT_NAME)
                .topic(TOPIC)
                .keys(event.getOrderNo())
                .sentTimeout(DEFAULT_TIMEOUT_MS)
                .delayTime(event.getDelayTime())
                .build();
    }

    @Override
    protected Message<?> buildMessage(OrderDelayCloseEvent event, BaseSendExtendDTO requestParam) {
        MessageWrapper<OrderDelayCloseEvent> wrapper = MessageWrapper.<OrderDelayCloseEvent>builder()
                .message(event)
                .keys(requestParam.getKeys())
                .timestamp(new Date())
                .build();
        return MessageBuilder.withPayload(JSON.toJSONString(wrapper))
                .setHeader("KEYS", requestParam.getKeys())
                .build();
    }
}
