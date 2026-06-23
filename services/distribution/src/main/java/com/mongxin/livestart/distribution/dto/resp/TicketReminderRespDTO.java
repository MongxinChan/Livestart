package com.mongxin.livestart.distribution.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * Reminder record response.
 */
@Data
public class TicketReminderRespDTO {

    private Long id;

    private Long eventId;

    private String eventTitle;

    private Integer ticketStage;

    private Integer status;

    private String statusDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date remindTime;

    private String reminderMessage;
}
