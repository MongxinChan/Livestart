package com.mongxin.livestart.engine.toolkit;

import cn.hutool.core.lang.UUID;

import java.util.Locale;

/**
 * 生成可定位到购票用户分片的电子票码。
 */
public final class TicketCheckCodeUtil {

    private TicketCheckCodeUtil() {
    }

    public static String generate(Long userId) {
        String userPart = Long.toString(userId, 36).toUpperCase(Locale.ROOT);
        return "T" + "0".repeat(13 - userPart.length()) + userPart
                + UUID.fastUUID().toString(true).substring(0, 18).toUpperCase(Locale.ROOT);
    }
}
