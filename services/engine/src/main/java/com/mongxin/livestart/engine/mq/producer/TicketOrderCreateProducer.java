package com.mongxin.livestart.engine.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.mq.base.BaseSendExtendDTO;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 购票异步下单（削峰）消息生产者
 */
@Component
public class TicketOrderCreateProducer extends AbstractCommonSendProduceTemplate<TicketOrderCreateEvent> {

    public static final String TOPIC = "livestart_engine_order-create_topic";
    private static final String EVENT_NAME = "订单异步创建";
    private static final long DEFAULT_TIMEOUT_MS = 3000L;

    public TicketOrderCreateProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(TicketOrderCreateEvent event) {
        return BaseSendExtendDTO.builder()
                .eventName(EVENT_NAME)
                .topic(TOPIC)
                .keys(event.getOrderNo())
                .sentTimeout(DEFAULT_TIMEOUT_MS)
                .delayTime(null) // 普通同步消息，不需要延时
                .build();
    }

    @Override
    protected Message<?> buildMessage(TicketOrderCreateEvent event, BaseSendExtendDTO requestParam) {
        MessageWrapper<TicketOrderCreateEvent> wrapper = MessageWrapper.<TicketOrderCreateEvent>builder()
                .message(event)
                .keys(requestParam.getKeys())
                .timestamp(new Date())
                .build();
        return MessageBuilder.withPayload(JSON.toJSONString(wrapper))
                .setHeader("KEYS", requestParam.getKeys())
                .build();
    }
}
