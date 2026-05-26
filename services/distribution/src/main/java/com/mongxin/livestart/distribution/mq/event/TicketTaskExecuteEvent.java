package com.mongxin.livestart.distribution.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门票批量推送任务异步执行事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTaskExecuteEvent {

    /**
     * 批量赠票分发任务主键ID
     */
    private Long taskId;
}
