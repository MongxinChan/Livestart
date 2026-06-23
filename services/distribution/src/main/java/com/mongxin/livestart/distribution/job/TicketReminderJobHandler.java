package com.mongxin.livestart.distribution.job;

import com.mongxin.livestart.distribution.service.TicketReminderService;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketReminderJobHandler {

    private final TicketReminderService ticketReminderService;
    private final XxlJobApiService xxlJobApiService;

    @XxlJob("ticketReminderJobHandler")
    public void execute() {
        String reminderIdStr = XxlJobHelper.getJobParam();
        XxlJobHelper.log("Scheduled ticket reminder triggered. reminderId={0}", reminderIdStr);
        if (reminderIdStr == null || reminderIdStr.isBlank()) {
            XxlJobHelper.handleFail("Missing job param: reminderId");
            return;
        }

        Long reminderId;
        try {
            reminderId = Long.parseLong(reminderIdStr.trim());
        } catch (NumberFormatException ex) {
            XxlJobHelper.handleFail("Invalid reminderId: " + reminderIdStr);
            return;
        }

        ticketReminderService.executeReminder(reminderId);

        try {
            long jobId = XxlJobHelper.getJobId();
            if (jobId > 0) {
                xxlJobApiService.removeJob((int) jobId);
            }
        } catch (Exception e) {
            log.warn("[Ticket Reminder] Failed to clean up one-shot xxl-job.", e);
        }

        XxlJobHelper.handleSuccess();
    }
}
