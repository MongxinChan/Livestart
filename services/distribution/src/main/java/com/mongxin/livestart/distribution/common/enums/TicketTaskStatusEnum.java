package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 大批量发票推送任务状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum TicketTaskStatusEnum {

    /**
     * 待执行
     */
    PENDING(0, "待执行"),

    /**
     * 执行中
     */
    RUNNING(1, "执行中"),

    /**
     * 已完成
     */
    COMPLETED(2, "已完成"),

    /**
     * 失败
     */
    FAILED(3, "失败");

    private final int code;
    private final String desc;
}
