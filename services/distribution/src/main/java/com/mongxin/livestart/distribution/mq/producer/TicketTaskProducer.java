package com.mongxin.livestart.distribution.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.distribution.mq.base.BaseSendExtendDTO;
import com.mongxin.livestart.distribution.mq.base.MessageWrapper;
import com.mongxin.livestart.distribution.mq.event.TicketTaskExecuteEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 门票批量赠送任务异步执行消息发送者
 */
@Component
public class TicketTaskProducer extends AbstractCommonSendProduceTemplate<TicketTaskExecuteEvent> {

    public static final String TOPIC = "livestart_distribution_ticket-task-execute_topic";
    private static final String EVENT_NAME = "门票批量发票赠送任务";
    private static final long DEFAULT_TIMEOUT_MS = 3000L;

    public TicketTaskProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(TicketTaskExecuteEvent event) {
        return BaseSendExtendDTO.builder()
                .eventName(EVENT_NAME)
                .topic(TOPIC)
                .keys(String.valueOf(event.getTaskId()))
                .sentTimeout(DEFAULT_TIMEOUT_MS)
                .build();
    }

    @Override
    protected Message<?> buildMessage(TicketTaskExecuteEvent event, BaseSendExtendDTO requestParam) {
        MessageWrapper<TicketTaskExecuteEvent> wrapper = MessageWrapper.<TicketTaskExecuteEvent>builder()
                .message(event)
                .keys(requestParam.getKeys())
                .timestamp(new Date())
                .build();
        return MessageBuilder.withPayload(JSON.toJSONString(wrapper))
                .setHeader("KEYS", requestParam.getKeys())
                .build();
    }
}
