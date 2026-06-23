package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Reminder lifecycle status.
 */
@Getter
@RequiredArgsConstructor
public enum TicketReminderStatusEnum {

    PENDING(0, "Pending"),
    REMINDED(1, "Reminded"),
    CANCELED(2, "Canceled");

    private final int code;
    private final String desc;
}
