package com.mongxin.livestart.distribution.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mongxin.livestart.distribution.mq.base.MessageWrapper;
import com.mongxin.livestart.distribution.mq.event.OrderPaySuccessEvent;
import com.mongxin.livestart.distribution.service.ArtistCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 监听普通门票售票支付成功出票通知，算艺人分销推广分成与劳务个税
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "livestart_engine_order-pay-success_topic",
        consumerGroup = "livestart_distribution_order-pay-success_cg"
)
@RequiredArgsConstructor
public class OrderPaySuccessConsumer implements RocketMQListener<String> {

    private final ArtistCommissionService artistCommissionService;

    @Override
    public void onMessage(String message) {
        log.info("[分销消费者] 监听到购票出票支付成功，正在核算艺人渠道分成比例及劳务税费... 消息：{}", message);

        try {
            MessageWrapper<OrderPaySuccessEvent> wrapper = JSON.parseObject(
                    message, new TypeReference<MessageWrapper<OrderPaySuccessEvent>>() {});
            OrderPaySuccessEvent event = wrapper.getMessage();

            // 执行生成待到账分成明细与个税算账
            artistCommissionService.processOrderPaySuccess(event);
        } catch (Exception e) {
            log.error("[分销消费者] 票房出票分成和税费处理异常，数据：{}", message, e);
            throw e;
        }
    }
}
