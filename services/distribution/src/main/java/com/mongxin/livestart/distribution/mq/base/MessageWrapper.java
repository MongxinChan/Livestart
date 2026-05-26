package com.mongxin.livestart.distribution.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 消息体幂等消费包装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageWrapper<T> {

    /**
     * 实际业务实体事件
     */
    private T message;

    /**
     * 唯一追踪 Key
     */
    private String keys;

    /**
     * 生产者投递时间
     */
    private Date timestamp;
}
