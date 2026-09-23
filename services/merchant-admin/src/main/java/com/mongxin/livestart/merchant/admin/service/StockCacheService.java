package com.mongxin.livestart.merchant.admin.service;

import com.mongxin.livestart.merchant.admin.common.constant.MerchantAdminRedisConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockCacheService {

    private static final String ENGINE_STOCK_KEY = "engine:stock:sku:%d";
    private static final DefaultRedisScript<Long> ADJUST_EXISTING_STOCK = new DefaultRedisScript<>(
            "for i = 1, #KEYS do " +
                    "if redis.call('EXISTS', KEYS[i]) == 1 then " +
                    "redis.call('INCRBY', KEYS[i], ARGV[1]) end end return 1",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public void initializeAfterCommit(Long skuId, int stock) {
        afterCommit(() -> {
            try {
                for (String key : stockKeys(skuId)) {
                    stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(stock));
                }
            } catch (Exception ex) {
                log.error("票种库存缓存预热失败，skuId={}", skuId, ex);
                invalidate(skuId);
            }
        });
    }

    public void adjustAfterCommit(Long skuId, int delta) {
        if (delta == 0) {
            return;
        }
        afterCommit(() -> {
            try {
                Long result = stringRedisTemplate.execute(ADJUST_EXISTING_STOCK,
                        stockKeys(skuId), String.valueOf(delta));
                if (result == null || result != 1L) {
                    throw new IllegalStateException("库存缓存调整失败");
                }
            } catch (Exception ex) {
                log.error("票种库存缓存调整失败，skuId={}，delta={}", skuId, delta, ex);
                invalidate(skuId);
            }
        });
    }

    public void invalidateAfterCommit(Long skuId) {
        afterCommit(() -> invalidate(skuId));
    }

    private void invalidate(Long skuId) {
        try {
            stringRedisTemplate.delete(stockKeys(skuId));
        } catch (Exception ex) {
            log.error("票种库存缓存失效失败，skuId={}", skuId, ex);
        }
    }

    private List<String> stockKeys(Long skuId) {
        return List.of(String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, skuId),
                String.format(ENGINE_STOCK_KEY, skuId));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
