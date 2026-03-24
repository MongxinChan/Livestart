package com.mongxin.livestart.framework.config;

import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmitAspect;
import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsumeAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等组件相关配置类
 */
public class IdempotentConfiguration {

    /**
     * 防止用户重复提交表单信息切面控制器
     */
    @Bean
    public NoDuplicateSubmitAspect noDuplicateSubmitAspect(RedissonClient redissonClient) {
        return new NoDuplicateSubmitAspect(redissonClient);
    }

    /**
     * 防止消息队列消费者重复消费消息切面控制器
     */
    @Bean
    public NoMQDuplicateConsumeAspect noMQDuplicateConsumeAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoMQDuplicateConsumeAspect(stringRedisTemplate);
    }
}
