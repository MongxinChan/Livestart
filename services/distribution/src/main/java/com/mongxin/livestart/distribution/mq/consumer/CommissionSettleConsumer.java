package com.mongxin.livestart.distribution.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mongxin.livestart.distribution.mq.base.MessageWrapper;
import com.mongxin.livestart.distribution.mq.event.CommissionSettleEvent;
import com.mongxin.livestart.distribution.service.ArtistCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 票房分成异步个税代扣及正式结算消费者
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "livestart.distribution.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_distribution_commission-settle_topic",
        consumerGroup = "livestart_distribution_commission-settle_cg"
)
@RequiredArgsConstructor
public class CommissionSettleConsumer implements RocketMQListener<String> {

    private final ArtistCommissionService artistCommissionService;

    @Override
    public void onMessage(String message) {
        log.info("[分销消费者] 接收到票房提成结算变更通知：{}", message);

        try {
            MessageWrapper<CommissionSettleEvent> wrapper = JSON.parseObject(
                    message, new TypeReference<MessageWrapper<CommissionSettleEvent>>() {});
            CommissionSettleEvent event = wrapper.getMessage();

            // 执行异步代扣税和票房入账
            artistCommissionService.settleCommission(event);
            log.info("[分销消费者] 票房分成个税结算成功，recordId={}，action={}",
                    event.getCommissionRecordId(), event.getAction());
        } catch (Exception e) {
            log.error("[分销消费者] 票房分成个税结算异常，数据：{}", message, e);
            throw e;
        }
    }
}
