package com.mongxin.livestart.distribution.service;

/**
 * XXL-JOB Admin OpenAPI 客户端，用于动态注册和管理定时任务
 */
public interface XxlJobApiService {

    /**
     * 动态注册一次性定时任务：在指定时间触发门票开售
     *
     * @param eventId       演出ID（作为任务执行参数）
     * @param eventTitle    演出标题（用于任务描述）
     * @param triggerTime   触发时间的 Cron 表达式
     * @return xxl-job 任务ID
     */
    int addTicketReleaseJob(Long eventId, String eventTitle, String triggerTime);

    /**
     * 移除/禁用已完成的一次性任务
     *
     * @param jobId xxl-job 任务ID
     */
    void removeJob(int jobId);
}
