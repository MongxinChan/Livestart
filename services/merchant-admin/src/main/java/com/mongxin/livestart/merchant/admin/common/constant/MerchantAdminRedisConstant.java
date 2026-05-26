package com.mongxin.livestart.merchant.admin.common.constant;

/**
 * 商户后台 Redis Key 常量
 */
public final class MerchantAdminRedisConstant {

    /**
     * 演出详情缓存 Key（Hash 结构）
     * 参数：演出 ID
     */
    public static final String EVENT_DETAIL_KEY = "livestart:event:detail:%d";

    /**
     * 票种库存缓存 Key（String 结构）
     * 参数：票种 ID
     */
    public static final String TICKET_STOCK_KEY = "livestart:ticket:stock:%d";

    private MerchantAdminRedisConstant() {
    }
}
