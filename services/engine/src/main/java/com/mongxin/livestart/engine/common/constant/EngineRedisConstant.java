package com.mongxin.livestart.engine.common.constant;

/**
 * 购票引擎 Redis Key 常量
 */
public final class EngineRedisConstant {

    /**
     * 票种库存 Key（由 merchant-admin 服务在票种创建时预热）
     * TICKET_STOCK_KEY % skuId
     */
    public static final String TICKET_STOCK_KEY = "engine:stock:sku:%d";

    /**
     * 用户对某场演出的购票次数限制 Key
     * USER_TICKET_LIMIT_KEY % userId, eventId
     */
    public static final String USER_TICKET_LIMIT_KEY = "engine:limit:user:%s:event:%d";

    /**
     * 订单分布式锁 Key
     * LOCK_ORDER_KEY % orderNo
     */
    public static final String LOCK_ORDER_KEY = "engine:lock:order:%s";

    /**
     * 购票幂等 Key（防重复下单）
     * IDEMPOTENT_ORDER_KEY % userId, skuId
     */
    public static final String IDEMPOTENT_ORDER_KEY = "engine:idempotent:order:%s:%d";

    /**
     * 滑动窗口限流 Key
     * RATE_LIMIT_KEY % userId, uri
     */
    public static final String RATE_LIMIT_KEY = "engine:ratelimit:%s:%s";

    private EngineRedisConstant() {
    }
}
