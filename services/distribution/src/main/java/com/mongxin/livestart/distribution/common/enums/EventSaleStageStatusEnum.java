package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event sale stage lifecycle status.
 */
@Getter
@RequiredArgsConstructor
public enum EventSaleStageStatusEnum {

    PENDING(0, "待开售"),
    OPENED(1, "已开售"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String desc;
}
