package com.mongxin.livestart.engine.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.mq.base.BaseSendExtendDTO;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 支付成功（出票通知）消息生产者
 */
@Component
public class OrderPaySuccessProducer extends AbstractCommonSendProduceTemplate<OrderPaySuccessEvent> {

    private static final String TOPIC = "livestart_engine_order-pay-success_topic";
    private static final String EVENT_NAME = "订单支付成功出票";
    private static final long DEFAULT_TIMEOUT_MS = 3000L;

    public OrderPaySuccessProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(OrderPaySuccessEvent event) {
        return BaseSendExtendDTO.builder()
                .eventName(EVENT_NAME)
                .topic(TOPIC)
                .keys(event.getOrderNo())
                .sentTimeout(DEFAULT_TIMEOUT_MS)
                .build();
    }

    @Override
    protected Message<?> buildMessage(OrderPaySuccessEvent event, BaseSendExtendDTO requestParam) {
        MessageWrapper<OrderPaySuccessEvent> wrapper = MessageWrapper.<OrderPaySuccessEvent>builder()
                .message(event)
                .keys(requestParam.getKeys())
                .timestamp(new Date())
                .build();
        return MessageBuilder.withPayload(JSON.toJSONString(wrapper))
                .setHeader("KEYS", requestParam.getKeys())
                .build();
    }
}
