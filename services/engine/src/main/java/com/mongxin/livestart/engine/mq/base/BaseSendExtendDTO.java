package com.mongxin.livestart.engine.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送扩展参数基础实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseSendExtendDTO {

    /**
     * 事件名称（用于日志）
     */
    private String eventName;

    /**
     * Topic
     */
    private String topic;

    /**
     * Tag（可选）
     */
    private String tag;

    /**
     * 消息 Keys（用于消息追踪和幂等）
     */
    private String keys;

    /**
     * 发送超时时间（ms）
     */
    private Long sentTimeout;

    /**
     * 延时投递时间戳（ms）。不为空时发送延时消息
     */
    private Long delayTime;
}
