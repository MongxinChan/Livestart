package com.mongxin.livestart.distribution.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mongxin.livestart.distribution.mq.base.MessageWrapper;
import com.mongxin.livestart.distribution.mq.event.TicketTaskExecuteEvent;
import com.mongxin.livestart.distribution.service.basics.DistributionStrategyChoose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 门票大批量推送任务异步执行消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "livestart_distribution_ticket-task-execute_topic",
        consumerGroup = "livestart_distribution_ticket-task-execute_cg"
)
@RequiredArgsConstructor
public class TicketTaskExecuteConsumer implements RocketMQListener<String> {

    private final DistributionStrategyChoose distributionStrategyChoose;

    @Override
    public void onMessage(String message) {
        log.info("[分销消费者] 接收到大批量赠票异步任务消息：{}", message);

        try {
            MessageWrapper<TicketTaskExecuteEvent> wrapper = JSON.parseObject(
                    message, new TypeReference<MessageWrapper<TicketTaskExecuteEvent>>() {});
            TicketTaskExecuteEvent event = wrapper.getMessage();

            // 路由到对应的策略实现
            distributionStrategyChoose.chooseAndExecute("ticket_task_execute_strategy", event);
            log.info("[分销消费者] 批量赠票异步任务执行成功，taskId={}", event.getTaskId());
        } catch (Exception e) {
            log.error("[分销消费者] 批量赠票异步任务执行失败，数据：{}", message, e);
            throw e;
        }
    }
}
