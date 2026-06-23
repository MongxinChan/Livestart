package com.mongxin.livestart.distribution.service;

/**
 * Client abstraction for dynamic XXL-JOB registration and cleanup.
 */
public interface XxlJobApiService {

    /**
     * Register a one-shot release job for the given event.
     */
    int addTicketReleaseJob(Long eventId, String eventTitle, String triggerTime);

    /**
     * Register a one-shot reminder job for the given reminder record.
     */
    int addTicketReminderJob(Long reminderId, String eventTitle, String triggerTime);

    /**
     * Remove a completed job from XXL-JOB admin.
     */
    void removeJob(int jobId);
}
