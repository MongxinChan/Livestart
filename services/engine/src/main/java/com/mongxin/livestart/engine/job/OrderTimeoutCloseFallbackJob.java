package com.mongxin.livestart.engine.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.service.StockRestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCloseFallbackJob {

    private static final int ORDER_DATABASES_COUNT = 2;
    private static final int ORDER_TABLES_COUNT = 16;

    private final JdbcTemplate jdbcTemplate;
    private final OrderItemMapper orderItemMapper;
    private final StockRestoreService stockRestoreService;

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
        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, row.orderNo())
                .eq(OrderItemDO::getUserId, row.userId()));
        if (items.isEmpty()) {
            log.warn("[超时关单兜底] 订单明细为空，跳过关单，orderNo={}", row.orderNo());
            return false;
        }

        Long skuId = items.get(0).getSkuId();
        Long eventId = items.get(0).getEventId();
        int count = items.size();
        try {
            boolean closed = stockRestoreService.closeTimeoutOrder(
                    row.id(), row.userId(), row.orderNo(), eventId, skuId, count);
            if (!closed) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.error("[超时关单兜底] 创建库存补偿任务失败，orderNo={}", row.orderNo(), ex);
            return false;
        }
    }

    private record TimeoutOrderRow(Long id, String orderNo, Long userId) {
    }
}
