package com.mongxin.livestart.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.mq.base.BaseSendExtendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

/**
 * RocketMQ 抽象公共发送消息组件
 * <p>
 * 封装延时消息与普通消息的统一发送逻辑，子类只需实现：
 * - buildBaseSendExtendParam：构建消息元数据
 * - buildMessage：构建消息体
 */
@Slf4j(topic = "AbstractCommonSendProduceTemplate")
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送扩展参数
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * 构建消息（含 Header、Keys 等）
     */
    protected abstract Message<?> buildMessage(T messageSendEvent, BaseSendExtendDTO requestParam);

    /**
     * 统一消息发送入口
     *
     * @param messageSendEvent 业务事件
     * @return 发送结果
     */
    public SendResult sendMessage(T messageSendEvent) {
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;
        try {
            // 构建 Topic 落点: topicName 或 topicName:tag
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            // 延时不为空时发送任意时间延时消息，否则发送普通同步消息
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

            log.info("[生产者] {} - 发送结果：{}，消息ID：{}，消息Keys：{}",
                    baseSendExtendDTO.getEventName(),
                    sendResult.getSendStatus(),
                    sendResult.getMsgId(),
                    baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            log.error("[生产者] {} - 消息发送失败，消息体：{}",
                    baseSendExtendDTO.getEventName(),
                    JSON.toJSONString(messageSendEvent), ex);
            throw ex;
        }

        return sendResult;
    }
}
