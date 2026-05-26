package com.mongxin.livestart.distribution.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.distribution.mq.base.BaseSendExtendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

/**
 * RocketMQ 抽象公共发送消息基类模板
 */
@Slf4j(topic = "AbstractCommonSendProduceTemplate")
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMQTemplate getRocketMQTemplate() {
        return this.rocketMQTemplate;
    }

    /**
     * 构建消息投递扩展属性参数
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * 构建消息
     */
    protected abstract Message<?> buildMessage(T messageSendEvent, BaseSendExtendDTO requestParam);

    /**
     * 统一同步消息投递
     *
     * @param messageSendEvent 业务实体事件
     * @return 投递发送结果
     */
    public SendResult sendMessage(T messageSendEvent) {
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;
        try {
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            if (baseSendExtendDTO.getDelayTime() != null) {
                sendResult = rocketMQTemplate.syncSendDeliverTimeMills(
                        destinationBuilder.toString(),
                        buildMessage(messageSendEvent, baseSendExtendDTO),
                        baseSendExtendDTO.getDelayTime()
                );
            } else {
                sendResult = rocketMQTemplate.syncSend(
                        destinationBuilder.toString(),
                        buildMessage(messageSendEvent, baseSendExtendDTO),
                        baseSendExtendDTO.getSentTimeout()
                );
            }

            log.info("[分销生产者] {} - 投递成功，状态：{}，消息ID：{}，Keys：{}",
                    baseSendExtendDTO.getEventName(),
                    sendResult.getSendStatus(),
                    sendResult.getMsgId(),
                    baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            log.error("[分销生产者] {} - 投递失败，数据：{}",
                    baseSendExtendDTO.getEventName(),
                    JSON.toJSONString(messageSendEvent), ex);
            throw ex;
        }

        return sendResult;
    }
}
