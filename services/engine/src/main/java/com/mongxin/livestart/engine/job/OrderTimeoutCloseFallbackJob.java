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

        int closedCount = closeTimeoutPendingOrdersInShards();
        if (closedCount > 0) {
            log.info("[超时关单兜底] 本轮关闭待支付订单数量={}", closedCount);
        }
    }

    /**
     * 使用逻辑表查询，让 ShardingSphere 广播到全部订单分片。
     * 不能在 ShardingSphere 数据源上直接写 ds_order_0.t_order_0 这类物理库表名，
     * 否则会被当成未知逻辑库并在定时任务中持续抛出 UnknownDatabaseException。
     */
    private int closeTimeoutPendingOrdersInShards() {
        String sql = """
                SELECT id, order_no, user_id
                FROM t_order
                WHERE status = ? AND create_time <= DATE_SUB(NOW(), INTERVAL ? MINUTE)
                ORDER BY create_time ASC
                LIMIT ?
                """;
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
