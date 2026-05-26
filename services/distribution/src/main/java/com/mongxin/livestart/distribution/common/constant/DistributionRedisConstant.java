package com.mongxin.livestart.distribution.common.constant;

/**
 * 门票抢购与分销模块 Redis 缓存常量定义
 */
public final class DistributionRedisConstant {

    private DistributionRedisConstant() {}

    /**
     * 演唱会门票剩余秒杀库存 Redis Key 前缀
     * 占位符: 门票票档 SkuID
     */
    public static final String TICKET_STOCK_KEY = "livestart:distribution:ticket:stock:%s";

    /**
     * 用户领票/购票限额 Redis Key 前缀
     * 占位符: 门票票档 SkuID, 用户ID
     */
    public static final String TICKET_USER_LIMIT_KEY = "livestart:distribution:ticket:limit:%s:user:%s";

    /**
     * 艺人推广专属宣发码与艺人 ID 映射缓存 Redis Key 前缀
     * 占位符: 宣发推广码
     */
    public static final String ARTIST_PROMO_CODE_KEY = "livestart:distribution:artist:promo:%s";

    /**
     * 生成艺人宣发码并发分布式锁 Key 前缀
     * 占位符: 艺人ID
     */
    public static final String ARTIST_CODE_GEN_LOCK = "livestart:distribution:artist:lock:gen:%s";
}
