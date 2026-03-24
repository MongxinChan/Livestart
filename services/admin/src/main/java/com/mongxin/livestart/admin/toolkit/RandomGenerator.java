package com.mongxin.livestart.admin.toolkit;

import cn.hutool.core.util.RandomUtil;

/**
 * @author Mongxin
 */
public final class RandomGenerator {

    /**
     * 字符池：0-9A-Za-z
     */
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private RandomGenerator() {
    }

    /**
     * 生成 6 位随机字符串
     */
    public static String generateRandom() {
        return generateRandom(6);
    }

    /**
     * 生成指定长度随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String generateRandom(int length) {
        return RandomUtil.randomString(CHARACTERS, length);
    }
}