package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaySuccessConsumerTest {

    private static final String NOTIFY_KEY = "engine:order:pay-success:notification:O-1";

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private OrderPaySuccessConsumer consumer;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        consumer = new OrderPaySuccessConsumer(stringRedisTemplate);
    }

    @Test
    void shouldMarkNotificationCompletedAfterProcessing() {
        when(valueOperations.setIfAbsent(NOTIFY_KEY, "PROCESSING", 5, TimeUnit.MINUTES)).thenReturn(true);

        consumer.onMessage(message());

        verify(valueOperations).set(NOTIFY_KEY, "COMPLETED", 30, TimeUnit.DAYS);
    }

    @Test
    void shouldSkipMessageAlreadyCompleted() {
        when(valueOperations.setIfAbsent(NOTIFY_KEY, "PROCESSING", 5, TimeUnit.MINUTES)).thenReturn(false);
        when(valueOperations.get(NOTIFY_KEY)).thenReturn("COMPLETED");

        consumer.onMessage(message());

        verify(valueOperations, never()).set(NOTIFY_KEY, "COMPLETED", 30, TimeUnit.DAYS);
    }

    @Test
    void shouldRetryWhenAnotherConsumerIsStillProcessing() {
        when(valueOperations.setIfAbsent(NOTIFY_KEY, "PROCESSING", 5, TimeUnit.MINUTES)).thenReturn(false);
        when(valueOperations.get(NOTIFY_KEY)).thenReturn("PROCESSING");

        assertThrows(IllegalStateException.class, () -> consumer.onMessage(message()));
    }

    private String message() {
        OrderPaySuccessEvent event = OrderPaySuccessEvent.builder()
                .orderNo("O-1")
                .userId(7L)
                .tradeNo("T-1")
                .build();
        return JSON.toJSONString(MessageWrapper.<OrderPaySuccessEvent>builder()
                .keys("O-1")
                .message(event)
                .timestamp(new Date())
                .build());
    }
}
