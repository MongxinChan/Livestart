package com.mongxin.livestart.engine.job;

import cn.hutool.core.lang.Singleton;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.service.impl.TicketOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCloseFallbackJob {

    private static final String STOCK_ROLLBACK_LUA_PATH = "lua/stock_rollback.lua";
    private static final int ORDER_DATABASES_COUNT = 2;
    private static final int ORDER_TABLES_COUNT = 16;

    private final JdbcTemplate jdbcTemplate;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${livestart.engine.order-timeout-fallback.enabled:true}")
    private boolean fallbackEnabled;

    @Value("${livestart.engine.order-close-delay-minutes:15}")
    private long orderCloseDelayMinutes;

    @Value("${livestart.engine.order-timeout-fallback.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${livestart.engine.order-timeout-fallback.fixed-delay-ms:60000}")
    public void closeTimeoutPendingOrders() {
        if (!fallbackEnabled) {
            return;
        }

        int closedCount = 0;
        for (int databaseIndex = 0; databaseIndex < ORDER_DATABASES_COUNT; databaseIndex++) {
            for (int tableIndex = 0; tableIndex < ORDER_TABLES_COUNT; tableIndex++) {
                closedCount += closeTimeoutPendingOrdersInShard(databaseIndex, tableIndex);
            }
        }
        if (closedCount > 0) {
            log.info("[超时关单兜底] 本轮关闭待支付订单数量={}", closedCount);
        }
    }

    private int closeTimeoutPendingOrdersInShard(int databaseIndex, int tableIndex) {
        String sql = String.format("""
                SELECT id, order_no, user_id
                FROM ds_order_%d.t_order_%d
                WHERE status = ? AND create_time <= DATE_SUB(NOW(), INTERVAL ? MINUTE)
                ORDER BY create_time ASC
                LIMIT ?
                """, databaseIndex, tableIndex);
        List<TimeoutOrderRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new TimeoutOrderRow(
                rs.getLong("id"),
                rs.getString("order_no"),
                rs.getLong("user_id")
        ), OrderStatusEnum.PENDING_PAYMENT.getCode(), orderCloseDelayMinutes, Math.max(batchSize, 1));

        int closedCount = 0;
        for (TimeoutOrderRow row : rows) {
            if (closeOneOrder(row)) {
                closedCount++;
            }
        }
        return closedCount;
    }

    private boolean closeOneOrder(TimeoutOrderRow row) {
        int affected = orderMapper.updateOrderStatus(
                row.id(),
                row.userId(),
                OrderStatusEnum.CANCELLED.getCode(),
                OrderStatusEnum.PENDING_PAYMENT.getCode()
        );
        if (!SqlHelper.retBool(affected)) {
            return false;
        }

        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, row.orderNo())
                .eq(OrderItemDO::getUserId, row.userId()));
        if (items.isEmpty()) {
            log.warn("[超时关单兜底] 订单明细为空，仅关闭订单，orderNo={}", row.orderNo());
            return true;
        }

        Long skuId = items.get(0).getSkuId();
        Long eventId = items.get(0).getEventId();
        int count = items.size();
        rollbackRedisStock(row.userId(), eventId, skuId, count, row.orderNo());
        ticketSkuMapper.returnStock(skuId, count);
        TicketOrderServiceImpl.releaseSoldOutMark(skuId);
        return true;
    }

    private void rollbackRedisStock(Long userId, Long eventId, Long skuId, int count, String orderNo) {
        try {
            String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, skuId);
            String userLimitKey = String.format(EngineRedisConstant.USER_TICKET_LIMIT_KEY, userId, eventId);
            stringRedisTemplate.execute(
                    loadLongRedisScript(STOCK_ROLLBACK_LUA_PATH),
                    List.of(stockKey, userLimitKey),
                    String.valueOf(count),
                    String.valueOf(count)
            );
        } catch (Exception ex) {
            log.error("[超时关单兜底] Redis 库存与限购计数回滚失败，orderNo={}", orderNo, ex);
        }
    }

    private DefaultRedisScript<Long> loadLongRedisScript(String classpath) {
        return Singleton.get(classpath, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(classpath)));
            script.setResultType(Long.class);
            return script;
        });
    }

    private record TimeoutOrderRow(Long id, String orderNo, Long userId) {
    }
}
