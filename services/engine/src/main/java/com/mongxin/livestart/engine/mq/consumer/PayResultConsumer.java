package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "livestart.engine.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_pay_pay-success_topic",
        consumerGroup = "livestart_engine_pay-result_cg"
)
public class PayResultConsumer implements RocketMQListener<String> {

    private final TicketOrderService ticketOrderService;

    @Override
    @NoMQDuplicateConsume(keyPrefix = "engine:idempotent:mq:pay-result:", key = "#message")
    public void onMessage(String message) {
        PayResultMessage event = JSON.parseObject(message, PayResultMessage.class);
        if (event == null || event.orderNo() == null) {
            throw new IllegalArgumentException("支付成功消息缺少订单号");
        }
        log.info("[支付结果] 收到支付服务事件，orderNo={}, tradeNo={}", event.orderNo(), event.tradeNo());
        ticketOrderService.paySuccess(event.orderNo(), event.tradeNo(), event.payAmount());
    }

    public record PayResultMessage(String eventId, String paySn, String orderNo,
                                   Long userId, String tradeNo, BigDecimal payAmount) {
    }
}
