package com.mongxin.livestart.engine.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MQ 消息包装器（包含消息元数据，用于幂等消费）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageWrapper<T> {

    /**
     * 消息体（业务事件）
     */
    private T message;

    /**
     * 消息唯一 ID（用于幂等）
     */
    private String keys;

    /**
     * 消息发送时间
     */
    private Date timestamp;
}
