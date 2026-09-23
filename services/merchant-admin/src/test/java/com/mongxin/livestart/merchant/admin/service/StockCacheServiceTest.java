package com.mongxin.livestart.merchant.admin.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCacheServiceTest {

    private static final List<String> STOCK_KEYS = List.of(
            "livestart:ticket:stock:42", "engine:stock:sku:42");

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldAdjustExistingCacheOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        StockCacheService service = new StockCacheService(stringRedisTemplate);

        service.adjustAfterCommit(42L, 5);

        verifyNoInteractions(stringRedisTemplate);
        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, callbacks.size());
        when(stringRedisTemplate.execute(any(RedisScript.class), eq(STOCK_KEYS), eq("5")))
                .thenReturn(1L);

        callbacks.forEach(TransactionSynchronization::afterCommit);

        verify(stringRedisTemplate).execute(any(RedisScript.class), eq(STOCK_KEYS), eq("5"));
    }

    @Test
    void shouldInvalidateCacheWhenAdjustmentFails() {
        StockCacheService service = new StockCacheService(stringRedisTemplate);
        when(stringRedisTemplate.execute(any(RedisScript.class), eq(STOCK_KEYS), eq("5")))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        service.adjustAfterCommit(42L, 5);

        verify(stringRedisTemplate).delete(STOCK_KEYS);
    }
}
