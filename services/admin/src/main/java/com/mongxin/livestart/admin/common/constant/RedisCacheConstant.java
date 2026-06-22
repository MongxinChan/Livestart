package com.mongxin.livestart.admin.common.constant;

/**
 * Livestart Admin 模块 Redis 缓存 Key 常量定义
 */
public class RedisCacheConstant {

    /**
     * 用户手机号注册分布式锁 Key 前缀
     * 用途：防止同一手机号并发重复注册，在注册流程中通过 Redisson 加锁
     */
    public static final String LOCK_USER_REGISTER_KEY = "live-start:lock_user-register:";

    /**
     * 活动/演出分组创建分布式锁 Key 前缀
     */
    public static final String LOCK_GROUP_CREATE_KEY = "live-start:lock_group-create:";

    /**
     * 用户登录态主存 Key 前缀
     * 结构：String，Key = USER_LOGIN_KEY + token，Value = 用户信息 JSON
     * 网关仅依赖 token 维度做鉴权
     */
    public static final String USER_LOGIN_KEY = "live-start:login:";

    /**
     * 用户登录态反向索引 Key 前缀
     * 结构：String，Key = USER_LOGIN_PHONE_INDEX_KEY + phone，Value = 当前活跃 token
     * 用于通过手机号失效旧会话（登出、用户类型变更等）
     */
    public static final String USER_LOGIN_PHONE_INDEX_KEY = "live-start:login:user:";
}
