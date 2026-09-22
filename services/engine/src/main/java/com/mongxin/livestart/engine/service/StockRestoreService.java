package com.mongxin.livestart.engine.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.StockRestoreTaskDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.StockRestoreTaskMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.remote.PayRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;

/**
 * 退款与超时关单库存回补的可靠执行器。数据库与 Redis 分别记录完成标记，重复执行不会重复增加库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRestoreService {
    private static final String REFUND_BIZ_TYPE = "REFUND";
    private static final String TIMEOUT_CLOSE_BIZ_TYPE = "TIMEOUT_CLOSE";
    private static final String REDIS_RESTORE_LUA = "lua/stock_restore_once.lua";
    private static final long REDIS_MARKER_TTL_SECONDS = 30L * 24 * 60 * 60;

    private final StockRestoreTaskMapper taskMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PayRemoteService payRemoteService;

    @Value("${livestart.engine.internal-token:change-me}")
    private String internalToken;

    @Value("${livestart.engine.stock-restore.retry-base-delay-ms:5000}")
    private long retryBaseDelayMs;

    public StockRestoreTaskDO prepareRefund(String orderNo, Long userId, Long eventId,
                                            Long skuId, int restoreCount) {
        return prepareTask(REFUND_BIZ_TYPE, orderNo, userId, eventId, skuId, restoreCount, 0);
    }

    /**
     * 创建超时关单库存回补任务。任务创建具有业务幂等性，同一订单只会有一条任务。
     */
    public StockRestoreTaskDO prepareTimeoutClose(String orderNo, Long userId, Long eventId,
                                                   Long skuId, int restoreCount) {
        return prepareTask(TIMEOUT_CLOSE_BIZ_TYPE, orderNo, userId, eventId, skuId, restoreCount, 1);
    }

    private StockRestoreTaskDO prepareTask(String bizType, String orderNo, Long userId, Long eventId,
                                           Long skuId, int restoreCount, int confirmed) {
        if (orderNo == null || userId == null || eventId == null || skuId == null || restoreCount <= 0) {
            throw new IllegalArgumentException("库存回补任务参数不完整");
        }
        StockRestoreTaskDO existing = taskMapper.selectOne(Wrappers.lambdaQuery(StockRestoreTaskDO.class)
                .eq(StockRestoreTaskDO::getBizType, bizType)
                .eq(StockRestoreTaskDO::getOrderNo, orderNo));
        if (existing != null) {
            return existing;
        }

        Date now = new Date();
        StockRestoreTaskDO task = StockRestoreTaskDO.builder()
                .bizType(bizType)
                .orderNo(orderNo)
                .userId(userId)
                .eventId(eventId)
                .skuId(skuId)
                .restoreCount(restoreCount)
                .refundConfirmed(confirmed)
                .dbRestored(0)
                .redisRestored(0)
                .status(0)
                .retryCount(0)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            taskMapper.insert(task);
            return task;
        } catch (Exception duplicate) {
            StockRestoreTaskDO concurrent = taskMapper.selectOne(Wrappers.lambdaQuery(StockRestoreTaskDO.class)
                    .eq(StockRestoreTaskDO::getBizType, bizType)
                    .eq(StockRestoreTaskDO::getOrderNo, orderNo));
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }
    }

    /**
     * 关闭订单并创建库存回补任务。库存回补失败时订单保持取消状态，任务由定时器继续重试。
     */
    public boolean closeTimeoutOrder(Long orderId, Long userId, String orderNo,
                                     Long eventId, Long skuId, int count) {
        StockRestoreTaskDO task = prepareTimeoutClose(orderNo, userId, eventId, skuId, count);
        int affected = orderMapper.updateOrderStatus(orderId, userId,
                OrderStatusEnum.CANCELLED.getCode(), OrderStatusEnum.PENDING_PAYMENT.getCode());
        if (affected <= 0) {
            OrderDO latest = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderNo, orderNo)
                    .eq(OrderDO::getUserId, userId));
            if (latest == null || latest.getStatus() != OrderStatusEnum.CANCELLED.getCode()) {
                // 订单已被支付或其他流程接管，不应再回补库存。
                if (task.getId() != null) {
                    taskMapper.deleteById(task.getId());
                }
                return false;
            }
        }
        processTimeoutClose(task);
        return true;
    }

    public void process(StockRestoreTaskDO task) {
        if (task == null || Integer.valueOf(1).equals(task.getStatus())) {
            return;
        }
        if (!Integer.valueOf(1).equals(task.getRefundConfirmed())) {
            if (!confirmRefundFromPayService(task)) {
                scheduleRetry(task, "等待支付服务确认退款结果");
                return;
            }
        }
        OrderDO order = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, task.getOrderNo())
                .eq(OrderDO::getUserId, task.getUserId()));
        if (order == null) {
            scheduleRetry(task, "订单不存在，无法完成退款状态迁移");
            return;
        }
        if (order.getStatus() != OrderStatusEnum.REFUNDED.getCode()) {
            int affected = orderMapper.updateOrderStatus(order.getId(), order.getUserId(),
                    OrderStatusEnum.REFUNDED.getCode(), OrderStatusEnum.PAID.getCode());
            if (!SqlHelper.retBool(affected)) {
                OrderDO latest = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                        .eq(OrderDO::getOrderNo, task.getOrderNo())
                        .eq(OrderDO::getUserId, task.getUserId()));
                if (latest == null || latest.getStatus() != OrderStatusEnum.REFUNDED.getCode()) {
                    scheduleRetry(task, "订单退款状态迁移失败");
                    return;
                }
            }
        }

        if (order.getStatus() != OrderStatusEnum.REFUNDED.getCode()
                && !Integer.valueOf(1).equals(task.getRefundConfirmed())) {
            scheduleRetry(task, "订单尚未完成退款状态");
            return;
        }

        restoreInventory(task);
    }

    /**
     * 处理超时关单任务。订单必须已经是取消状态，避免支付与关单并发时误回补库存。
     */
    public void processTimeoutClose(StockRestoreTaskDO task) {
        if (task == null || Integer.valueOf(1).equals(task.getStatus())) {
            return;
        }
        OrderDO order = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, task.getOrderNo())
                .eq(OrderDO::getUserId, task.getUserId()));
        if (order == null) {
            scheduleRetry(task, "订单不存在，无法确认超时关单");
            return;
        }
        if (order.getStatus() != OrderStatusEnum.CANCELLED.getCode()) {
            if (order.getStatus() == OrderStatusEnum.PENDING_PAYMENT.getCode()) {
                scheduleRetry(task, "订单尚未完成关单");
            } else {
                taskMapper.deleteById(task.getId());
            }
            return;
        }
        restoreInventory(task);
    }

    public boolean confirmRefund(StockRestoreTaskDO task) {
        if (task == null || task.getId() == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(task.getRefundConfirmed())) {
            return true;
        }
        boolean confirmed = SqlHelper.retBool(taskMapper.markRefundConfirmed(task.getId()));
        if (confirmed) {
            task.setRefundConfirmed(1);
        }
        return confirmed;
    }

    @Scheduled(fixedDelayString = "${livestart.engine.stock-restore.fixed-delay-ms:10000}")
    public void retryPending() {
        for (StockRestoreTaskDO task : taskMapper.selectPending()) {
            try {
                if (TIMEOUT_CLOSE_BIZ_TYPE.equals(task.getBizType())) {
                    processTimeoutClose(task);
                } else {
                    process(task);
                }
            } catch (Exception ex) {
                log.error("[库存补偿] 任务执行异常，orderNo={}, taskId={}", task.getOrderNo(), task.getId(), ex);
                scheduleRetry(task, "任务执行异常：" + safeMessage(ex));
            }
        }
    }

    private void restoreInventory(StockRestoreTaskDO task) {
        try {
            restoreDatabase(task.getId());
        } catch (Exception ex) {
            log.error("[库存补偿] 数据库库存回补失败，orderNo={}, taskId={}", task.getOrderNo(), task.getId(), ex);
            scheduleRetry(task, "数据库库存回补失败：" + safeMessage(ex));
            return;
        }

        try {
            restoreRedis(task);
        } catch (Exception ex) {
            log.error("[库存补偿] Redis 库存回补失败，orderNo={}, taskId={}", task.getOrderNo(), task.getId(), ex);
            scheduleRetry(task, "Redis 库存回补失败：" + safeMessage(ex));
            return;
        }

        taskMapper.markCompleted(task.getId());
        releaseSoldOutMark(task.getSkuId());
        log.info("[库存补偿] 库存回补完成，bizType={}, orderNo={}, taskId={}, count={}",
                task.getBizType(), task.getOrderNo(), task.getId(), task.getRestoreCount());
    }

    private boolean confirmRefundFromPayService(StockRestoreTaskDO task) {
        try {
            var result = payRemoteService.refundStatus(task.getOrderNo(), internalToken);
            if (result != null && result.isSuccess() && result.getData() != null
                    && Integer.valueOf(1).equals(result.getData().getStatus())) {
                return confirmRefund(task);
            }
        } catch (Exception ex) {
            log.warn("[库存补偿] 查询支付服务退款状态失败，orderNo={}", task.getOrderNo(), ex);
        }
        return false;
    }

    private void restoreDatabase(Long taskId) {
        transactionTemplate.executeWithoutResult(status -> {
            StockRestoreTaskDO locked = taskMapper.selectForUpdate(taskId);
            if (locked == null || Integer.valueOf(1).equals(locked.getStatus())
                    || Integer.valueOf(1).equals(locked.getDbRestored())) {
                return;
            }
            int affected = ticketSkuMapper.returnStock(locked.getSkuId(), locked.getRestoreCount());
            if (!SqlHelper.retBool(affected)) {
                status.setRollbackOnly();
                throw new IllegalStateException("票档库存记录不存在");
            }
            int marked = taskMapper.markDbRestored(taskId);
            if (!SqlHelper.retBool(marked)) {
                status.setRollbackOnly();
                throw new IllegalStateException("数据库库存补偿标记失败");
            }
        });
    }

    private void restoreRedis(StockRestoreTaskDO task) {
        StockRestoreTaskDO latest = taskMapper.selectById(task.getId());
        if (latest == null || Integer.valueOf(1).equals(latest.getRedisRestored())) {
            return;
        }
        String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, task.getSkuId());
        String userLimitKey = String.format(EngineRedisConstant.USER_TICKET_LIMIT_KEY,
                task.getUserId(), task.getEventId());
        String markerKey = "engine:stock:restore:" + task.getBizType().toLowerCase() + ":" + task.getOrderNo();
        Long result = stringRedisTemplate.execute(
                loadRestoreScript(),
                List.of(stockKey, userLimitKey, markerKey),
                String.valueOf(task.getRestoreCount()),
                String.valueOf(task.getRestoreCount()),
                String.valueOf(REDIS_MARKER_TTL_SECONDS));
        if (result == null) {
            throw new IllegalStateException("Redis 补偿脚本无返回值");
        }
        taskMapper.markRedisRestored(task.getId());
    }

    private void scheduleRetry(StockRestoreTaskDO task, String error) {
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        long multiplier = 1L << Math.min(retryCount, 6);
        long delay = Math.min(300_000L, Math.max(1_000L, retryBaseDelayMs) * multiplier);
        taskMapper.scheduleRetry(task.getId(), new Date(System.currentTimeMillis() + delay), error);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private DefaultRedisScript<Long> loadRestoreScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(REDIS_RESTORE_LUA)));
        script.setResultType(Long.class);
        return script;
    }

    private void releaseSoldOutMark(Long skuId) {
        if (skuId != null) {
            com.mongxin.livestart.engine.service.impl.TicketOrderServiceImpl.releaseSoldOutMark(skuId);
        }
    }
}
