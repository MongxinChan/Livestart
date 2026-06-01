package com.mongxin.livestart.engine.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解（基于 Redis 滑动窗口）
 * <p>
 * 标注在 Controller 方法上，限制单个用户在指定时间窗口内的最大请求次数。
 * 超出限额时拦截器将直接返回 HTTP 429。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求数
     */
    int permits() default 5;

    /**
     * 滑动窗口大小（毫秒），默认 1 秒
     */
    long timeWindowMs() default 1000;
}
