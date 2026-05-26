package com.mongxin.livestart.distribution.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息投递扩展属性 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseSendExtendDTO {

    /**
     * 业务事件描述 (用于日志监控)
     */
    private String eventName;

    /**
     * 投递 Topic 主题
     */
    private String topic;

    /**
     * 投递 Tag
     */
    private String tag;

    /**
     * 消息唯一 Key (用于消息链路跟踪和消费端幂等)
     */
    private String keys;

    /**
     * 发送超时时间 (ms)
     */
    private Long sentTimeout;

    /**
     * 延时投递时间戳 (ms)，为非空时将调用 RocketMQ 延时同步发送
     */
    private Long delayTime;
}
