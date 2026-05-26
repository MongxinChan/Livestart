package com.mongxin.livestart.merchant.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 演出状态枚举
 */
@Getter
@AllArgsConstructor
public enum EventStatusEnum {

    OFF_SHELF(0, "下架"),
    PRESALE(1, "预售"),
    ON_SALE(2, "在售"),
    SOLD_OUT(3, "售罄");

    private final int status;
    private final String description;
}
