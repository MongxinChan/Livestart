package com.mongxin.harmony.admin.common.constant;

/***
 * 短链接后管 Redis 缓存常量类
 */
public class RedisCacheConstant {


    /**
     * 用户注册分布式锁
     */
    public static final String LOCK_USER_REGISTER_KEY = "live-start:lock_user-register:";

    /***
     * 分布创建分布锁
     */
    public static final String LOCK_GROUP_CREATE_KEY = "live-start:lock_group-create:";

    /**
     * 用户登录信息 Hash
     */
    public static final String USER_LOGIN_KEY = "live-start:login:";
}
