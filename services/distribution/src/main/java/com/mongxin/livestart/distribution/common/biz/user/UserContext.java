package com.mongxin.livestart.distribution.common.biz.user;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 线程安全用户上下文 (使用 TransmittableThreadLocal 防止线程池环境下发生数据混淆)
 */
public final class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new TransmittableThreadLocal<>();

    private UserContext() {}

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
     * 清理上下文，防范内存泄露
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
