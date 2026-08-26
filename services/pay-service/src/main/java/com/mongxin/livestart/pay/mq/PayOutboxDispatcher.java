package com.mongxin.livestart.pay.mq;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.pay.dao.entity.PayOutboxDO;
import com.mongxin.livestart.pay.dao.mapper.PayOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayOutboxDispatcher {
    private final PayOutboxMapper outboxMapper;
    private final PayResultProducer producer;

    @Scheduled(fixedDelayString = "${livestart.pay.outbox.fixed-delay-ms:5000}")
    public void dispatch() {
        List<PayOutboxDO> pending = outboxMapper.selectPending();
        for (PayOutboxDO outbox : pending) {
            try {
                PayResultEvent event = JSON.parseObject(outbox.getPayload(), PayResultEvent.class);
                SendResult result = producer.send(event);
                if (result != null && "SEND_OK".equals(result.getSendStatus().name())) {
                    outboxMapper.markSent(outbox.getId());
                } else {
                    scheduleRetry(outbox);
                }
            } catch (Exception e) {
                log.error("[支付Outbox] 投递失败，eventId={}", outbox.getEventId(), e);
                scheduleRetry(outbox);
            }
        }
    }

    private void scheduleRetry(PayOutboxDO outbox) {
        long delay = Math.min(300_000L, 5_000L * (1L << Math.min(outbox.getRetryCount(), 6)));
        outboxMapper.scheduleRetry(outbox.getId(), new Date(System.currentTimeMillis() + delay));
    }
}
