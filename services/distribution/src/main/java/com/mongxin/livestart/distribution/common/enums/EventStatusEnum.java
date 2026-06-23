package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event sale status used by the distribution service.
 */
@Getter
@RequiredArgsConstructor
public enum EventStatusEnum {

    /**
     * Published but not on sale yet.
     */
    PENDING_SALE(1, "Presale"),

    /**
     * Stock has been preheated to Redis and users can start grabbing tickets.
     */
    ON_SALE(2, "On sale"),

    /**
     * Event has finished or the ticketing lifecycle has ended.
     */
    FINISHED(3, "Finished");

    private final int code;
    private final String desc;
}
