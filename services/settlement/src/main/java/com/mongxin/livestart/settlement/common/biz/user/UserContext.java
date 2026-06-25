package com.mongxin.livestart.settlement.common.biz.user;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 结算服务用户上下文
 */
public final class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new TransmittableThreadLocal<>();

    private UserContext() {
    }

    public static void setUser(UserInfoDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static String getUserId() {
        return Optional.ofNullable(USER_THREAD_LOCAL.get()).map(UserInfoDTO::getUserId).orElse(null);
    }

    public static Integer getUserType() {
        return Optional.ofNullable(USER_THREAD_LOCAL.get()).map(UserInfoDTO::getUserType).orElse(null);
    }

    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
