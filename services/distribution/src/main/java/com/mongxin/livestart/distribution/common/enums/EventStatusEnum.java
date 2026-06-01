package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 演出状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum EventStatusEnum {

    /**
     * 待开售（已发布但尚未到开售时间）
     */
    PENDING_SALE(0, "待开售"),

    /**
     * 已开售（XXL-JOB 定时触发后，库存预热至 Redis，用户可抢票）
     */
    ON_SALE(1, "已开售"),

    /**
     * 已结束
     */
    FINISHED(2, "已结束");

    private final int code;
    private final String desc;
}
