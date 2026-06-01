package com.mongxin.livestart.engine.common.biz.user;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 用户上下文（与 admin 服务同构，独立定义）
 * <p>
 * 使用 TransmittableThreadLocal 保证线程池场景下的上下文传递
 */
public final class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new TransmittableThreadLocal<>();

    public static void setUser(UserInfoDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     * 获取用户ID
     */
    public static String getUserId() {
        return Optional.ofNullable(USER_THREAD_LOCAL.get())
                .map(UserInfoDTO::getUserId)
                .orElse(null);
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        return Optional.ofNullable(USER_THREAD_LOCAL.get())
                .map(UserInfoDTO::getUsername)
                .orElse(null);
    }

    /**
     * 获取手机号
     */
    public static String getPhone() {
        return Optional.ofNullable(USER_THREAD_LOCAL.get())
                .map(UserInfoDTO::getPhone)
                .orElse(null);
    }

    /**
     * 清理上下文（Filter 的 finally 块中调用）
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
