package com.mongxin.livestart.distribution.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.distribution.mq.base.BaseSendExtendDTO;
import com.mongxin.livestart.distribution.mq.base.MessageWrapper;
import com.mongxin.livestart.distribution.mq.event.CommissionSettleEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 艺人票房提成及个税代扣延迟/异步结算消息生产者
 */
@Slf4j
@Component
public class CommissionSettleProducer extends AbstractCommonSendProduceTemplate<CommissionSettleEvent> {

    public static final String TOPIC = "livestart_distribution_commission-settle_topic";
    private static final String EVENT_NAME = "艺人票房提成个税结算变更";
    private static final long DEFAULT_TIMEOUT_MS = 3000L;

    public CommissionSettleProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CommissionSettleEvent event) {
        return BaseSendExtendDTO.builder()
                .eventName(EVENT_NAME)
                .topic(TOPIC)
                .keys(String.valueOf(event.getCommissionRecordId()))
                .sentTimeout(DEFAULT_TIMEOUT_MS)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CommissionSettleEvent event, BaseSendExtendDTO requestParam) {
        MessageWrapper<CommissionSettleEvent> wrapper = MessageWrapper.<CommissionSettleEvent>builder()
                .message(event)
                .keys(requestParam.getKeys())
                .timestamp(new Date())
                .build();
        return MessageBuilder.withPayload(JSON.toJSONString(wrapper))
                .setHeader("KEYS", requestParam.getKeys())
                .build();
    }

    /**
     * 发送延时到期正式结算消息 (15 天延迟)
     *
     * @param event 结算事件
     * @param delayMills 延迟毫秒数
     */
    public void sendDelayMessage(CommissionSettleEvent event, long delayMills) {
        BaseSendExtendDTO extendDTO = buildBaseSendExtendParam(event);
        extendDTO.setDelayTime(System.currentTimeMillis() + delayMills);

        StringBuilder destinationBuilder = new StringBuilder(extendDTO.getTopic());
        if (extendDTO.getTag() != null) {
            destinationBuilder.append(":").append(extendDTO.getTag());
        }

        try {
            SendResult sendResult = getRocketMQTemplate().syncSendDeliverTimeMills(
                    destinationBuilder.toString(),
                    buildMessage(event, extendDTO),
                    extendDTO.getDelayTime()
            );

            log.info("[延时生产者] {} - 成功发送延迟结算：{}，ID：{}，结算期投递戳：{}",
                    extendDTO.getEventName(), sendResult.getSendStatus(),
                    sendResult.getMsgId(), extendDTO.getDelayTime());
        } catch (Throwable ex) {
            log.error("[延时生产者] {} - 发送延迟结算异常", extendDTO.getEventName(), ex);
            throw ex;
        }
    }
}
