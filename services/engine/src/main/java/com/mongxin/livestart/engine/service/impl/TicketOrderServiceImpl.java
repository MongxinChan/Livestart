package com.mongxin.livestart.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.engine.common.biz.user.UserContext;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.common.enums.StockDecrementErrorEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.entity.StockRestoreTaskDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.dao.mapper.RefundPolicyMapper;
import com.mongxin.livestart.engine.dto.req.AdminOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderRefundReqDTO;
import com.mongxin.livestart.engine.dto.resp.AdminOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderDetailRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyRecordRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyStatsRespDTO;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.remote.AdminRemoteService;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.PayRemoteService;
import com.mongxin.livestart.engine.remote.dto.PayCreateRequestDTO;
import com.mongxin.livestart.engine.remote.dto.RefundCreateRequestDTO;
import com.mongxin.livestart.engine.remote.dto.AdminUserSimpleRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantEventRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantVenueRespDTO;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.engine.service.RefundPolicyEvaluator;
import com.mongxin.livestart.engine.service.StockRestoreService;
import com.mongxin.livestart.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.mongxin.livestart.engine.toolkit.TicketCheckCodeUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.framework.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.messaging.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketOrderServiceImpl implements TicketOrderService {

    private static final String STOCK_DECREMENT_LUA_PATH = "lua/stock_decrement.lua";
    private static final String STOCK_ROLLBACK_LUA_PATH = "lua/stock_rollback.lua";
    private static final long USER_LIMIT_KEY_EXPIRE_SECONDS = 7 * 24 * 3600L;
    private static final String PATH_TOKEN_KEY = "engine:pathtoken:%s:%s";
    private static final String SECRET_SALT = "LiveStart_Engine_PathToken_Salt_Key";
    private static final int USER_TYPE_VENUE_ADMIN = 3;
    private static final int USER_TYPE_SUPER_ADMIN = 4;
    private static final int EVENT_SCOPE_PAGE_SIZE = 200;
    private static final int EVENT_SCOPE_MAX_PAGE = 100;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final RefundPolicyMapper refundPolicyMapper;
    private final AdminRemoteService adminRemoteService;
    private final MerchantAdminRemoteService merchantAdminRemoteService;
    private final PayRemoteService payRemoteService;
    private final RefundPolicyEvaluator refundPolicyEvaluator;
    private final StockRestoreService stockRestoreService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderPaySuccessProducer orderPaySuccessProducer;
    private final TicketOrderCreateProducer ticketOrderCreateProducer;

    @Value("${livestart.engine.local-order-mode:false}")
    private boolean localOrderMode;

    @Value("${livestart.engine.auto-warm-stock-on-miss:true}")
    private boolean autoWarmStockOnMiss;

    @Value("${livestart.engine.mq.enabled:true}")
    private boolean mqEnabled;

    @Value("${livestart.engine.internal-token:change-me}")
    private String internalToken;

    @Override
    public String generatePathToken(Long skuId) {
        String userId = requireUserId();
        if (skuId == null) {
            throw new ClientException("票种不存在");
        }
        TicketSkuDO sku = loadTicketSku(skuId);
        if (sku == null) {
            log.warn("[下单Token] 票种不存在，拒绝生成 token，userId={}, skuId={}", userId, skuId);
            throw new ClientException("票种不存在");
        }

        log.info("[下单Token] 票种校验通过，userId={}, skuId={}, eventId={}, remainingStock={}",
                userId, skuId, sku.getEventId(), sku.getRemainingStock());
        if (sku.getRemainingStock() == null || sku.getRemainingStock() <= 0) {
            throw new ClientException("该票种已售罄");
        }
        ensureStockCacheWarm(sku);

        String tokenSource = userId + "_" + skuId + "_" + SECRET_SALT + "_" + UUID.fastUUID().toString(true);
        String pathToken = SecureUtil.md5(tokenSource);
        String tokenKey = String.format(PATH_TOKEN_KEY, userId, skuId);
        stringRedisTemplate.opsForValue().set(tokenKey, pathToken, 5, TimeUnit.SECONDS);
        return pathToken;
    }

    @Override
    public String createOrder(TicketOrderCreateReqDTO requestParam, String pathToken) {
        String userId = requireUserId();
        Long skuId = requestParam.getSkuId();
        validatePathToken(pathToken, userId, skuId);
        validateVisitorCount(requestParam);

        TicketSkuDO sku = loadTicketSku(skuId);
        if (sku == null) {
            log.warn("[下单] 票种不存在，创建订单失败，userId={}, skuId={}, visitorIds={}",
                    userId, skuId, requestParam.getVisitorIds());
            throw new ClientException("票种不存在");
        }

        log.info("[下单] 票种查询成功，userId={}, skuId={}, eventId={}, remainingStock={}, count={}",
                userId, skuId, sku.getEventId(), sku.getRemainingStock(), requestParam.getCount());
        if (sku.getRemainingStock() == null || sku.getRemainingStock() <= 0) {
            throw new ClientException("该票种已售罄");
        }

        return doCreateOrder(requestParam, sku, Long.parseLong(userId));
    }

    private String doCreateOrder(TicketOrderCreateReqDTO requestParam, TicketSkuDO sku, Long userId) {
        DefaultRedisScript<Long> decrementScript = loadLongRedisScript(STOCK_DECREMENT_LUA_PATH);
        String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, sku.getId());
        String userLimitKey = String.format(EngineRedisConstant.USER_TICKET_LIMIT_KEY, userId, sku.getEventId());
        int maxLimit = sku.getLimitNum() != null ? sku.getLimitNum() : 6;
        ensureStockCacheWarm(sku);

        Long luaResult = stringRedisTemplate.execute(
                decrementScript,
                List.of(stockKey, userLimitKey),
                String.valueOf(requestParam.getCount()),
                String.valueOf(maxLimit),
                String.valueOf(USER_LIMIT_KEY_EXPIRE_SECONDS)
        );
        if (luaResult == null) {
            throw new ServiceException("抢票服务繁忙，请稍后重试");
        }

        long errorCode = StockDecrementReturnCombinedUtil.extractErrorCode(luaResult);
        if (StockDecrementErrorEnum.isFail(errorCode)) {
            StockDecrementErrorEnum error = StockDecrementErrorEnum.fromCode(errorCode);
            throw new ServiceException(error.getMessage());
        }

        String orderNo = generateOrderNo(userId);
        TicketOrderCreateEvent createEvent = TicketOrderCreateEvent.builder()
                .orderNo(orderNo)
                .userId(userId)
                .skuId(sku.getId())
                .eventId(sku.getEventId())
                .count(requestParam.getCount())
                .visitorIds(requestParam.getVisitorIds())
                .build();

        if (shouldPersistOrderLocally()) {
            persistOrderDirectly(createEvent);
            log.info("[下单] 本地直写模式下单成功，userId={}, skuId={}, orderNo={}",
                    userId, requestParam.getSkuId(), orderNo);
            return orderNo;
        }

        SendResult sendResult;
        try {
            sendResult = ticketOrderCreateProducer.sendMessage(createEvent);
        } catch (Exception ex) {
            if (shouldFallbackToLocalOrder(ex)) {
                log.warn("[下单] RocketMQ Topic 路由缺失，回退本地直写下单，orderNo={}, skuId={}",
                        orderNo, requestParam.getSkuId(), ex);
                persistOrderDirectly(createEvent);
                return orderNo;
            }
            rollbackPreDeductStock(stockKey, userLimitKey, requestParam.getCount());
            throw ex;
        }
        if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
            log.error("[下单] 异步下单消息投递失败，开始回滚预扣资源，orderNo={}", orderNo);
            rollbackPreDeductStock(stockKey, userLimitKey, requestParam.getCount());
            throw new ServiceException("抢票排队人数较多，请稍后重试");
        }

        log.info("[下单] 异步下单消息投递成功，userId={}, skuId={}, orderNo={}",
                userId, requestParam.getSkuId(), orderNo);
        return orderNo;
    }

    @Override
    public String payWithAlipay(String orderNo) {
        String userId = requireUserId();
        var result = payRemoteService.create(
                new PayCreateRequestDTO(orderNo, "LiveStart 演出门票 - " + orderNo), userId);
        if (result == null || result.isFail() || result.getData() == null) {
            throw new ServiceException(result == null ? "支付服务不可用" : result.getMessage());
        }
        return result.getData().getBody();
    }

    @Override
    public void paySuccess(String orderNo, String tradeNo) {
        paySuccess(orderNo, tradeNo, null);
    }

    @Override
    public void paySuccess(String orderNo, String tradeNo, BigDecimal payAmount) {
        OrderDO order = getOrderByOrderNo(orderNo);
        if (order == null) {
            log.error("[支付成功通知] 订单不存在，orderNo={}", orderNo);
            throw new ClientException("订单不存在");
        }

        if (payAmount != null && order.getTotalAmount() != null
                && order.getTotalAmount().compareTo(payAmount) != 0) {
            log.error("[支付成功通知] 支付金额校验失败，orderNo={}, expected={}, actual={}",
                    orderNo, order.getTotalAmount(), payAmount);
            throw new ClientException("支付金额校验失败");
        }

        if (order.getStatus() == OrderStatusEnum.PAID.getCode()) {
            // 订单状态可能已在上一次尝试中提交，但出票消息投递失败。重复通知必须重发消息，不能直接丢弃。
            publishPaySuccessEvent(order, tradeNo);
            log.info("[支付成功通知] 订单已支付，已重发出票事件，orderNo={}", orderNo);
            return;
        }

        if (order.getStatus() == OrderStatusEnum.CANCELLED.getCode()) {
            // 关单与支付宝回调可能并发。订单已取消时不能吞掉已到账支付，改为幂等全额退款。
            BigDecimal refundAmount = payAmount != null ? payAmount : order.getTotalAmount();
            var refundResult = payRemoteService.refund(
                    new RefundCreateRequestDTO(orderNo, "订单已关单但支付成功，自动退款", refundAmount),
                    String.valueOf(order.getUserId()), internalToken);
            if (refundResult == null || refundResult.isFail() || refundResult.getData() == null
                    || !Integer.valueOf(1).equals(refundResult.getData().getStatus())) {
                log.error("[支付成功通知] 取消订单自动退款失败，orderNo={}, tradeNo={}", orderNo, tradeNo);
                throw new ServiceException("订单已取消，自动退款处理中");
            }
            log.warn("[支付成功通知] 订单已取消，已自动发起全额退款，orderNo={}, tradeNo={}", orderNo, tradeNo);
            return;
        }

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("[支付成功通知] 当前订单状态不允许支付成功流转，orderNo={}, status={}",
                    orderNo, order.getStatus());
            throw new ClientException("订单状态异常，无法处理支付");
        }

        transactionTemplate.executeWithoutResult(status -> {
            try {
                int affected = orderMapper.updateOrderStatus(
                        order.getId(),
                        order.getUserId(),
                        OrderStatusEnum.PAID.getCode(),
                        OrderStatusEnum.PENDING_PAYMENT.getCode()
                );
                if (!SqlHelper.retBool(affected)) {
                    throw new ServiceException("支付通知处理失败，订单状态已变更");
                }
                int payTimeAffected = orderMapper.updatePayTime(order.getId(), order.getUserId(), new Date());
                if (!SqlHelper.retBool(payTimeAffected)) {
                    throw new ServiceException("支付时间更新失败");
                }
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });

        publishPaySuccessEvent(order, tradeNo);
        log.info("[支付成功通知] 支付处理成功并已投递出票事件，orderNo={}", orderNo);
    }

    private void publishPaySuccessEvent(OrderDO order, String tradeNo) {
        if (shouldSkipMqSend()) {
            log.info("[支付成功通知] 本地直写模式已处理支付成功，跳过 RocketMQ 投递，orderNo={}", order.getOrderNo());
            return;
        }

        OrderPaySuccessEvent payEvent = OrderPaySuccessEvent.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .tradeNo(tradeNo)
                .build();
        try {
            SendResult sendResult = orderPaySuccessProducer.sendMessage(payEvent);
            if (sendResult == null || sendResult.getSendStatus() == null
                    || !"SEND_OK".equals(sendResult.getSendStatus().name())) {
                throw new ServiceException("支付成功出票消息投递失败，等待 MQ 重试");
            }
        } catch (Exception ex) {
            log.error("[支付成功通知] 支付成功出票消息发送失败，orderNo={}", order.getOrderNo(), ex);
            if (ex instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("支付成功出票消息投递失败，等待 MQ 重试");
        }
    }

    @Override
    public void cancelOrder(TicketOrderCancelReqDTO requestParam) {
        String userId = requireUserId();
        OrderDO order = getOrderByNo(requestParam.getOrderNo(), Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            throw new ClientException("仅待支付订单可以取消");
        }

        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, requestParam.getOrderNo())
                .eq(OrderItemDO::getUserId, order.getUserId()));
        int count = items.size();
        OrderItemDO firstItem = CollUtil.getFirst(items);
        if (firstItem == null || firstItem.getEventId() == null || firstItem.getSkuId() == null) {
            throw new ServiceException("订单明细不完整，暂不能取消订单");
        }

        boolean cancelled = stockRestoreService.closeTimeoutOrder(
                order.getId(), order.getUserId(), requestParam.getOrderNo(),
                firstItem.getEventId(), firstItem.getSkuId(), count);
        if (!cancelled) {
            throw new ServiceException("订单状态已变更，取消失败");
        }
        log.info("[取消订单] 订单已取消，orderNo={}", requestParam.getOrderNo());
    }

    @Override
    public void refundOrder(TicketOrderRefundReqDTO requestParam) {
        Instant requestedAt = Instant.now();
        String userId = requireUserId();
        OrderDO order = getOrderByNo(requestParam.getOrderNo(), Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PAID.getCode()) {
            throw new ClientException("仅已支付订单可以申请退票");
        }

        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, requestParam.getOrderNo())
                .eq(OrderItemDO::getUserId, order.getUserId()));
        if (items.stream().anyMatch(item -> Integer.valueOf(1).equals(item.getIsChecked()))) {
            throw new ClientException("已核销的电子票不能退票");
        }
        OrderItemDO firstItem = CollUtil.getFirst(items);
        if (firstItem == null || firstItem.getEventId() == null) {
            throw new ClientException("订单缺少演出信息，无法计算退票规则");
        }
        if (firstItem.getSkuId() == null || items.size() <= 0) {
            throw new ClientException("订单缺少票档信息，无法创建库存补偿任务");
        }
        RefundPolicyEvaluator.RefundDecision decision = refundPolicyEvaluator.evaluate(
                refundPolicyMapper.selectByEventId(firstItem.getEventId()),
                order.getTotalAmount(), requestedAt);
        if (!decision.allowed()) {
            throw new ClientException(decision.rejectionReason());
        }

        StockRestoreTaskDO restoreTask = stockRestoreService.prepareRefund(
                requestParam.getOrderNo(), order.getUserId(), firstItem.getEventId(),
                firstItem.getSkuId(), items.size());

        var refundResult = payRemoteService.refund(
                new RefundCreateRequestDTO(requestParam.getOrderNo(), requestParam.getReason(), decision.refundAmount()),
                userId, internalToken);
        if (refundResult == null || refundResult.isFail() || refundResult.getData() == null
                || !Integer.valueOf(1).equals(refundResult.getData().getStatus())) {
            throw new ServiceException(refundResult == null ? "支付服务不可用"
                    : (refundResult.getMessage() == null ? "退款失败" : refundResult.getMessage()));
        }

        if (!stockRestoreService.confirmRefund(restoreTask)) {
            throw new ServiceException("退款成功但库存补偿任务确认失败，请稍后重试");
        }

        int affected = orderMapper.updateOrderStatus(
                order.getId(),
                order.getUserId(),
                OrderStatusEnum.REFUNDED.getCode(),
                OrderStatusEnum.PAID.getCode()
        );
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("退票申请失败，请重试");
        }

        stockRestoreService.process(restoreTask);

        log.info("[退票] 退票成功，orderNo={}, tier={}, refundAmount={}, requestedAt={}, minutesBeforeStart={}",
                requestParam.getOrderNo(), decision.tier(), decision.refundAmount(), requestedAt,
                decision.minutesBeforeStart());
    }

    @Override
    public IPage<TicketOrderPageQueryRespDTO> pageQueryOrders(TicketOrderPageQueryReqDTO requestParam) {
        String userId = requireUserId();
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, Long.parseLong(userId))
                .eq(requestParam.getStatus() != null, OrderDO::getStatus, requestParam.getStatus())
                .orderByDesc(OrderDO::getCreateTime);
        IPage<OrderDO> page = orderMapper.selectPage(new Page<>(requestParam.getCurrent(), requestParam.getSize()), queryWrapper);
        Map<Long, MerchantTicketSkuDetailRespDTO> skuCache = new HashMap<>();
        Map<Long, MerchantEventRespDTO> eventCache = new HashMap<>();
        return page.convert(order -> {
            List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderNo, order.getOrderNo())
                    .eq(OrderItemDO::getUserId, order.getUserId()));
            OrderItemDO firstItem = CollUtil.getFirst(items);
            Long skuId = firstItem != null ? firstItem.getSkuId() : null;
            Long eventId = firstItem != null ? firstItem.getEventId() : null;
            MerchantTicketSkuDetailRespDTO sku = loadTicketSkuDetail(skuId, skuCache);
            MerchantEventRespDTO event = loadEventDetail(eventId, eventCache);

            TicketOrderPageQueryRespDTO dto = new TicketOrderPageQueryRespDTO();
            dto.setOrderNo(order.getOrderNo());
            dto.setEventId(eventId);
            dto.setEventTitle(event != null ? event.getTitle() : "");
            dto.setSkuTitle(sku != null ? sku.getTitle() : "");
            dto.setPrice(sku != null ? sku.getSellingPrice() : BigDecimal.ZERO);
            dto.setCount(items.size());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setStatus(order.getStatus());
            dto.setStatusDesc(OrderStatusEnum.fromCode(order.getStatus()).getDesc());
            dto.setCreateTime(order.getCreateTime());
            dto.setCheckCode(firstItem != null ? firstItem.getCheckCode() : null);
            dto.setIsChecked(firstItem != null ? firstItem.getIsChecked() : 0);
            return dto;
        });
    }

    @Override
    public IPage<AdminOrderPageQueryRespDTO> pageQueryAdminOrders(AdminOrderPageQueryReqDTO requestParam) {
        Integer userType = UserContext.getUserType();
        if (userType == null || (userType != USER_TYPE_SUPER_ADMIN && userType != USER_TYPE_VENUE_ADMIN)) {
            throw new ClientException("当前用户无后台订单查看权限");
        }

        long current = normalizeCurrent(requestParam.getCurrent());
        long size = normalizeSize(requestParam.getSize());
        Integer statusCode = requestParam.getStatus() == null ? null : toOrderStatusCode(requestParam.getStatus());
        List<Long> visibleEventIds = resolveAdminVisibleEventIds(userType, requestParam);
        if (visibleEventIds != null && CollUtil.isEmpty(visibleEventIds)) {
            return emptyAdminOrderPage(current, size);
        }

        IPage<OrderDO> orderPage = orderMapper.pageQueryAdminOrders(new Page<>(current, size), statusCode, visibleEventIds);
        List<AdminOrderPageQueryRespDTO> records = buildAdminOrderRecords(orderPage.getRecords());
        fillUsernames(records);
        Page<AdminOrderPageQueryRespDTO> page = new Page<>(current, size, orderPage.getTotal());
        page.setRecords(records);
        return page;
    }

    private List<Long> resolveAdminVisibleEventIds(Integer userType, AdminOrderPageQueryReqDTO requestParam) {
        Long requestEventId = requestParam.getEventId();
        Long requestVenueId = requestParam.getVenueId();
        if (userType == USER_TYPE_SUPER_ADMIN) {
            return resolveSuperAdminEventFilter(requestEventId, requestVenueId);
        }
        return resolveVenueAdminEventFilter(requestEventId, requestVenueId);
    }

    private List<Long> resolveSuperAdminEventFilter(Long requestEventId, Long requestVenueId) {
        if (requestEventId == null && requestVenueId == null) {
            return null;
        }
        if (requestEventId != null && requestVenueId == null) {
            return List.of(requestEventId);
        }
        if (requestEventId != null) {
            MerchantEventRespDTO event = loadEventDetail(requestEventId, new HashMap<>());
            return event != null && requestVenueId.equals(event.getVenueId()) ? List.of(requestEventId) : List.of();
        }
        return listAllMerchantEvents().stream()
                .filter(event -> requestVenueId.equals(event.getVenueId()))
                .map(MerchantEventRespDTO::getId)
                .toList();
    }

    private List<Long> resolveVenueAdminEventFilter(Long requestEventId, Long requestVenueId) {
        Long currentUserId = parseCurrentUserId();
        Map<Long, MerchantVenueRespDTO> venueCache = new HashMap<>();
        List<Long> manageableEventIds = listAllMerchantEvents().stream()
                .filter(event -> event != null && event.getId() != null && event.getVenueId() != null)
                .filter(event -> requestEventId == null || requestEventId.equals(event.getId()))
                .filter(event -> requestVenueId == null || requestVenueId.equals(event.getVenueId()))
                .filter(event -> isVenueManagedByCurrentUser(event.getVenueId(), currentUserId, venueCache))
                .map(MerchantEventRespDTO::getId)
                .toList();
        return CollUtil.isEmpty(manageableEventIds) ? List.of() : manageableEventIds;
    }

    private Long parseCurrentUserId() {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户未登录");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new ClientException("当前用户身份无效");
        }
    }

    private boolean isVenueManagedByCurrentUser(Long venueId, Long currentUserId, Map<Long, MerchantVenueRespDTO> venueCache) {
        MerchantVenueRespDTO venue = loadVenueDetail(venueId, venueCache);
        return venue != null && currentUserId.equals(venue.getOwnerUserId());
    }

    private List<MerchantEventRespDTO> listAllMerchantEvents() {
        List<MerchantEventRespDTO> events = new ArrayList<>();
        for (int current = 1; current <= EVENT_SCOPE_MAX_PAGE; current++) {
            Result<Page<MerchantEventRespDTO>> result = merchantAdminRemoteService.pageQueryEvents(current, EVENT_SCOPE_PAGE_SIZE);
            if (result == null || result.isFail() || result.getData() == null) {
                log.warn("[后台订单分页] 查询演出范围失败, current={}, result={}", current, result);
                throw new ServiceException("查询演出管理范围失败，请稍后重试");
            }
            Page<MerchantEventRespDTO> page = result.getData();
            if (CollUtil.isEmpty(page.getRecords())) {
                break;
            }
            events.addAll(page.getRecords());
            if (events.size() >= page.getTotal() || page.getCurrent() >= page.getPages()) {
                break;
            }
        }
        return events;
    }

    private Page<AdminOrderPageQueryRespDTO> emptyAdminOrderPage(long current, long size) {
        Page<AdminOrderPageQueryRespDTO> page = new Page<>(current, size, 0);
        page.setRecords(List.of());
        return page;
    }

    @Override
    public TicketVerifyStatsRespDTO getVerifyStats(Long eventId) {
        Integer userType = UserContext.getUserType();
        if (userType == null || (userType != USER_TYPE_SUPER_ADMIN && userType != USER_TYPE_VENUE_ADMIN)) {
            throw new ClientException("当前用户无验票统计查看权限");
        }

        AdminOrderPageQueryReqDTO scopeRequest = new AdminOrderPageQueryReqDTO();
        scopeRequest.setEventId(eventId);
        List<Long> visibleEventIds = resolveAdminVisibleEventIds(userType, scopeRequest);
        if (visibleEventIds != null && CollUtil.isEmpty(visibleEventIds)) {
            return buildVerifyStats(0L, 0L);
        }

        Long totalCount = orderItemMapper.countPaidTickets(visibleEventIds, false);
        Long checkedCount = orderItemMapper.countPaidTickets(visibleEventIds, true);
        return buildVerifyStats(totalCount, checkedCount);
    }

    private TicketVerifyStatsRespDTO buildVerifyStats(Long totalCount, Long checkedCount) {
        long total = totalCount == null ? 0L : totalCount;
        long checked = checkedCount == null ? 0L : checkedCount;
        TicketVerifyStatsRespDTO result = new TicketVerifyStatsRespDTO();
        result.setTotalCount(total);
        result.setCheckedCount(checked);
        result.setUncheckedCount(Math.max(total - checked, 0L));
        result.setCheckedRate(total == 0L
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(checked)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP));
        return result;
    }

    @Override
    public TicketVerifyRespDTO verifyTicket(String checkCode) {
        Integer userType = UserContext.getUserType();
        if (userType == null || (userType != USER_TYPE_SUPER_ADMIN && userType != USER_TYPE_VENUE_ADMIN)) {
            throw new ClientException("当前用户无验票权限");
        }
        String normalizedCode = StrUtil.trim(checkCode);
        if (StrUtil.isBlank(normalizedCode)) {
            throw new ClientException("电子票码不能为空");
        }

        Long ticketUserId = parseTicketUserId(normalizedCode);
        var ticketQuery = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getCheckCode, normalizedCode);
        if (ticketUserId != null) {
            ticketQuery.eq(OrderItemDO::getUserId, ticketUserId);
        }
        OrderItemDO item = orderItemMapper.selectOne(ticketQuery);
        if (item == null) {
            throw new ClientException("无效电子票码");
        }

        if (userType == USER_TYPE_VENUE_ADMIN) {
            MerchantEventRespDTO event = loadEventDetail(item.getEventId(), new HashMap<>());
            if (event == null || event.getVenueId() == null
                    || !isVenueManagedByCurrentUser(event.getVenueId(), parseCurrentUserId(), new HashMap<>())) {
                throw new ClientException("无权核验其他场馆的电子票");
            }
        }

        OrderDO order = getOrderByNo(item.getOrderNo(), item.getUserId());
        if (order == null || !OrderStatusEnum.PAID.equals(OrderStatusEnum.fromCode(order.getStatus()))) {
            throw new ClientException("该电子票尚未支付或订单已失效");
        }

        Date checkedAt = new Date();
        Long checkedBy = parseCurrentUserId();
        int affected = orderItemMapper.update(null, Wrappers.lambdaUpdate(OrderItemDO.class)
                .eq(OrderItemDO::getCheckCode, normalizedCode)
                .eq(OrderItemDO::getUserId, item.getUserId())
                .and(wrapper -> wrapper.eq(OrderItemDO::getIsChecked, 0)
                        .or()
                        .isNull(OrderItemDO::getIsChecked))
                .set(OrderItemDO::getIsChecked, 1)
                .set(OrderItemDO::getCheckedAt, checkedAt)
                .set(OrderItemDO::getCheckedBy, checkedBy));
        if (!SqlHelper.retBool(affected)) {
            throw new ClientException("该电子票已核验");
        }

        TicketVerifyRespDTO result = new TicketVerifyRespDTO();
        result.setOrderNo(item.getOrderNo());
        result.setCheckCode(item.getCheckCode());
        result.setEventId(item.getEventId());
        result.setSkuId(item.getSkuId());
        result.setVisitorId(item.getVisitorId());
        result.setStatus("已入场");
        result.setCheckedAt(checkedAt);
        result.setCheckedBy(checkedBy);
        log.info("[现场验票] 核验成功，orderNo={}, checkCode={}", item.getOrderNo(), normalizedCode);
        return result;
    }

    @Override
    public List<TicketVerifyRecordRespDTO> getRecentVerifyRecords(Long eventId) {
        Integer userType = UserContext.getUserType();
        if (userType == null || (userType != USER_TYPE_SUPER_ADMIN && userType != USER_TYPE_VENUE_ADMIN)) {
            throw new ClientException("当前用户无验票权限");
        }
        AdminOrderPageQueryReqDTO scopeRequest = new AdminOrderPageQueryReqDTO();
        scopeRequest.setEventId(eventId);
        List<Long> visibleEventIds = resolveAdminVisibleEventIds(userType, scopeRequest);
        if (visibleEventIds != null && visibleEventIds.isEmpty()) {
            return List.of();
        }
        var query = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getIsChecked, 1)
                .isNotNull(OrderItemDO::getCheckedAt)
                .orderByDesc(OrderItemDO::getCheckedAt)
                .last("LIMIT 10");
        if (visibleEventIds != null) {
            query.in(OrderItemDO::getEventId, visibleEventIds);
        }
        return orderItemMapper.selectList(query).stream().map(item -> {
            TicketVerifyRecordRespDTO record = new TicketVerifyRecordRespDTO();
            record.setId(item.getId());
            record.setOrderNo(item.getOrderNo());
            record.setCheckCode(item.getCheckCode());
            record.setEventId(item.getEventId());
            record.setCheckedAt(item.getCheckedAt());
            record.setCheckedBy(item.getCheckedBy());
            return record;
        }).toList();
    }
    @Override
    public TicketOrderDetailRespDTO getOrderDetail(String orderNo) {
        String userId = requireUserId();
        OrderDO order = getOrderByNo(orderNo, Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }

        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, orderNo)
                .eq(OrderItemDO::getUserId, order.getUserId()));

        TicketOrderDetailRespDTO dto = new TicketOrderDetailRespDTO();
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusDesc(OrderStatusEnum.fromCode(order.getStatus()).getDesc());
        dto.setPayTime(order.getPayTime());
        dto.setCreateTime(order.getCreateTime());
        dto.setTicketItems(items.stream().map(item -> {
            TicketOrderDetailRespDTO.TicketItemRespDTO ticketItem = new TicketOrderDetailRespDTO.TicketItemRespDTO();
            ticketItem.setId(item.getId());
            ticketItem.setVisitorId(item.getVisitorId());
            ticketItem.setCheckCode(item.getCheckCode());
            ticketItem.setIsChecked(item.getIsChecked());
            return ticketItem;
        }).collect(Collectors.toList()));
        return dto;
    }

    private void validatePathToken(String pathToken, String userId, Long skuId) {
        if (StrUtil.isBlank(pathToken)) {
            throw new ClientException("安全校验失败，下单请求无效");
        }
        String tokenKey = String.format(PATH_TOKEN_KEY, userId, skuId);
        String cachedToken = stringRedisTemplate.opsForValue().get(tokenKey);
        if (cachedToken == null || !cachedToken.equals(pathToken)) {
            throw new ClientException("安全校验失效，请重新发起下单");
        }
        stringRedisTemplate.delete(tokenKey);
    }

    private void validateVisitorCount(TicketOrderCreateReqDTO requestParam) {
        if (CollUtil.isEmpty(requestParam.getVisitorIds())
                || requestParam.getVisitorIds().size() != requestParam.getCount()) {
            throw new ClientException("观演人数量与购票数量不一致");
        }
    }

    private void rollbackPreDeductStock(String stockKey, String userLimitKey, int count) {
        DefaultRedisScript<Long> rollbackScript = loadLongRedisScript(STOCK_ROLLBACK_LUA_PATH);
        try {
            stringRedisTemplate.execute(
                    rollbackScript,
                    List.of(stockKey, userLimitKey),
                    String.valueOf(count),
                    String.valueOf(count)
            );
        } catch (Exception redisEx) {
            log.error("[下单] Redis 库存与限购回滚失败，stockKey={}, userLimitKey={}, count={}",
                    stockKey, userLimitKey, count, redisEx);
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

    private OrderDO getOrderByNo(String orderNo, Long userId) {
        return orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, orderNo)
                .eq(OrderDO::getUserId, userId));
    }

    private void persistOrderDirectly(TicketOrderCreateEvent event) {
        TicketSkuDO latestSku = loadTicketSku(event.getSkuId());
        if (latestSku == null) {
            throw new ServiceException("票档不存在");
        }

        boolean stockDecremented = decrementStockInSingleTx(latestSku, event.getCount());
        if (!stockDecremented) {
            throw new ServiceException("库存扣减失败");
        }

        try {
            persistOrderAndItemsInSingleTx(event, latestSku);
        } catch (Exception ex) {
            restoreStockAfterOrderFailure(latestSku.getId(), event.getCount());
            throw ex;
        }
    }

    private boolean decrementStockInSingleTx(TicketSkuDO latestSku, int count) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                int decremented = ticketSkuMapper.decrementStock(latestSku.getId(), count, latestSku.getVersion());
                return SqlHelper.retBool(decremented);
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        }));
    }

    private void persistOrderAndItemsInSingleTx(TicketOrderCreateEvent event, TicketSkuDO latestSku) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                BigDecimal totalAmount = latestSku.getSellingPrice().multiply(BigDecimal.valueOf(event.getCount()));
                Date now = new Date();
                OrderDO order = OrderDO.builder()
                        .orderNo(event.getOrderNo())
                        .userId(event.getUserId())
                        .totalAmount(totalAmount)
                        .status(OrderStatusEnum.PENDING_PAYMENT.getCode())
                        .createTime(now)
                        .build();
                orderMapper.insert(order);

                for (Long visitorId : event.getVisitorIds()) {
                    OrderItemDO item = OrderItemDO.builder()
                            .orderNo(event.getOrderNo())
                            .userId(event.getUserId())
                            .visitorId(visitorId)
                            .eventId(latestSku.getEventId())
                            .skuId(latestSku.getId())
                            .checkCode(TicketCheckCodeUtil.generate(event.getUserId()))
                            .isChecked(0)
                            .build();
                    orderItemMapper.insert(item);
                }
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
    }

    private void restoreStockAfterOrderFailure(Long skuId, int count) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                int affected = ticketSkuMapper.returnStock(skuId, count);
                if (!SqlHelper.retBool(affected)) {
                    throw new ServiceException("订单失败后的库存回补失败");
                }
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
    }

    private OrderDO getOrderByOrderNo(String orderNo) {
        return orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, orderNo));
    }

    private Long parseTicketUserId(String checkCode) {
        if (!checkCode.startsWith("T") || checkCode.length() != 32) {
            return null;
        }
        try {
            return Long.parseLong(checkCode.substring(1, 14), 36);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String requireUserId() {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户未登录");
        }
        return userId;
    }

    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userSuffix = String.format("%04d", userId % 10000);
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return timestamp + userSuffix + randomSuffix;
    }

    private boolean shouldPersistOrderLocally() {
        return localOrderMode || !mqEnabled;
    }

    private boolean shouldSkipMqSend() {
        return shouldPersistOrderLocally();
    }

    private boolean shouldFallbackToLocalOrder(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof MQClientException || current instanceof MessagingException) {
                String message = current.getMessage();
                if (StrUtil.containsIgnoreCase(message, "No route info of this topic")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private TicketSkuDO loadTicketSku(Long skuId) {
        Result<MerchantTicketSkuDetailRespDTO> result = merchantAdminRemoteService.getTicketSku(skuId);
        if (result == null || result.isFail() || result.getData() == null) {
            log.warn("[票种查询] 远程票种查询失败，skuId={}, result={}", skuId, result);
            return null;
        }
        return toTicketSkuDO(result.getData());
    }

    private TicketSkuDO toTicketSkuDO(MerchantTicketSkuDetailRespDTO data) {
        TicketSkuDO sku = new TicketSkuDO();
        sku.setId(data.getId());
        sku.setEventId(data.getEventId());
        sku.setTitle(data.getTitle());
        sku.setOriginalPrice(data.getOriginalPrice());
        sku.setSellingPrice(data.getSellingPrice());
        sku.setTotalStock(data.getTotalStock());
        sku.setRemainingStock(data.getRemainingStock());
        sku.setLimitNum(data.getLimitNum());
        sku.setVersion(data.getVersion());
        return sku;
    }

    private void ensureStockCacheWarm(TicketSkuDO sku) {
        if (!autoWarmStockOnMiss) {
            return;
        }
        String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, sku.getId());
        String cachedStock = stringRedisTemplate.opsForValue().get(stockKey);
        if (cachedStock != null) {
            return;
        }

        Integer remainingStock = sku.getRemainingStock() != null ? sku.getRemainingStock() : 0;
        Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(remainingStock));
        if (Boolean.TRUE.equals(initialized)) {
            log.info("[库存预热] engine 库存缓存缺失，已按数据库剩余库存完成预热，skuId={}, stock={}",
                    sku.getId(), remainingStock);
        }
    }

    private List<AdminOrderPageQueryRespDTO> buildAdminOrderRecords(List<OrderDO> orders) {
        if (CollUtil.isEmpty(orders)) {
            return List.of();
        }

        Map<Long, MerchantTicketSkuDetailRespDTO> skuCache = new HashMap<>();
        Map<Long, MerchantEventRespDTO> eventCache = new HashMap<>();
        Map<Long, MerchantVenueRespDTO> venueCache = new HashMap<>();
        List<AdminOrderPageQueryRespDTO> records = new ArrayList<>(orders.size());
        for (OrderDO order : orders) {
            records.add(buildAdminOrderRecord(order, skuCache, eventCache, venueCache));
        }
        return records;
    }

    private AdminOrderPageQueryRespDTO buildAdminOrderRecord(OrderDO order,
                                                             Map<Long, MerchantTicketSkuDetailRespDTO> skuCache,
                                                             Map<Long, MerchantEventRespDTO> eventCache,
                                                             Map<Long, MerchantVenueRespDTO> venueCache) {
        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, order.getOrderNo())
                .eq(OrderItemDO::getUserId, order.getUserId()));
        if (CollUtil.isEmpty(items)) {
            log.warn("[后台订单分页] 订单明细缺失，orderNo={}, userId={}", order.getOrderNo(), order.getUserId());
        }

        OrderItemDO firstItem = CollUtil.getFirst(items);
        Long skuId = firstItem != null ? firstItem.getSkuId() : null;
        Long eventId = firstItem != null ? firstItem.getEventId() : null;
        MerchantTicketSkuDetailRespDTO skuDetail = loadTicketSkuDetail(skuId, skuCache);
        MerchantEventRespDTO eventDetail = loadEventDetail(eventId, eventCache);
        Long venueId = eventDetail != null ? eventDetail.getVenueId() : null;
        MerchantVenueRespDTO venueDetail = loadVenueDetail(venueId, venueCache);

        AdminOrderPageQueryRespDTO dto = new AdminOrderPageQueryRespDTO();
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setEventId(eventId);
        dto.setEventTitle(eventDetail != null ? eventDetail.getTitle() : "");
        dto.setVenueId(venueId);
        dto.setVenueName(venueDetail != null ? venueDetail.getName() : "");
        dto.setSkuId(skuId);
        dto.setSkuTitle(skuDetail != null ? skuDetail.getTitle() : "");
        dto.setTicketCount(items.size());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusDesc(resolveStatusDesc(order.getStatus()));
        dto.setCreateTime(order.getCreateTime());
        return dto;
    }

    private MerchantTicketSkuDetailRespDTO loadTicketSkuDetail(Long skuId,
                                                               Map<Long, MerchantTicketSkuDetailRespDTO> skuCache) {
        if (skuId == null) {
            return null;
        }
        if (skuCache.containsKey(skuId)) {
            return skuCache.get(skuId);
        }

        Result<MerchantTicketSkuDetailRespDTO> result = merchantAdminRemoteService.getTicketSku(skuId);
        MerchantTicketSkuDetailRespDTO detail = null;
        if (result != null && result.isSuccess()) {
            detail = result.getData();
        } else {
            log.warn("[后台订单分页] 查询票档信息失败，skuId={}, code={}, message={}",
                    skuId,
                    result != null ? result.getCode() : "null",
                    result != null ? result.getMessage() : "remote result is null");
        }
        skuCache.put(skuId, detail);
        return detail;
    }

    private MerchantEventRespDTO loadEventDetail(Long eventId,
                                                 Map<Long, MerchantEventRespDTO> eventCache) {
        if (eventId == null) {
            return null;
        }
        if (eventCache.containsKey(eventId)) {
            return eventCache.get(eventId);
        }

        Result<MerchantEventRespDTO> result = merchantAdminRemoteService.getEvent(eventId);
        MerchantEventRespDTO detail = null;
        if (result != null && result.isSuccess()) {
            detail = result.getData();
        } else {
            log.warn("[后台订单分页] 查询演出信息失败，eventId={}, code={}, message={}",
                    eventId,
                    result != null ? result.getCode() : "null",
                    result != null ? result.getMessage() : "remote result is null");
        }
        eventCache.put(eventId, detail);
        return detail;
    }

    private MerchantVenueRespDTO loadVenueDetail(Long venueId,
                                                 Map<Long, MerchantVenueRespDTO> venueCache) {
        if (venueId == null) {
            return null;
        }
        if (venueCache.containsKey(venueId)) {
            return venueCache.get(venueId);
        }

        Result<MerchantVenueRespDTO> result = merchantAdminRemoteService.getVenue(venueId);
        MerchantVenueRespDTO detail = null;
        if (result != null && result.isSuccess()) {
            detail = result.getData();
        } else {
            log.warn("[后台订单分页] 查询场馆信息失败，venueId={}, code={}, message={}",
                    venueId,
                    result != null ? result.getCode() : "null",
                    result != null ? result.getMessage() : "remote result is null");
        }
        venueCache.put(venueId, detail);
        return detail;
    }

    private long normalizeCurrent(long current) {
        return current <= 0 ? 1 : current;
    }

    private long normalizeSize(long size) {
        return size <= 0 ? 10 : size;
    }

    private void fillUsernames(List<AdminOrderPageQueryRespDTO> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        List<Long> userIds = records.stream().map(AdminOrderPageQueryRespDTO::getUserId).distinct().toList();
        Result<List<AdminUserSimpleRespDTO>> result = adminRemoteService.listSimpleUsersByIds(userIds, internalToken);
        if (result == null || result.isFail() || CollUtil.isEmpty(result.getData())) {
            records.forEach(each -> each.setUsername(String.valueOf(each.getUserId())));
            return;
        }
        Map<Long, String> usernameMap = new HashMap<>();
        for (AdminUserSimpleRespDTO each : result.getData()) {
            String displayName = StrUtil.blankToDefault(each.getRealName(), each.getUsername());
            usernameMap.put(each.getId(), StrUtil.blankToDefault(displayName, String.valueOf(each.getId())));
        }
        records.forEach(each -> each.setUsername(usernameMap.getOrDefault(each.getUserId(), String.valueOf(each.getUserId()))));
    }

    private String resolveStatusDesc(Integer statusCode) {
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(statusCode);
        return statusEnum != null ? statusEnum.getDesc() : "";
    }

    private Integer toOrderStatusCode(Integer adminStatus) {
        return switch (adminStatus) {
            case 1 -> OrderStatusEnum.PENDING_PAYMENT.getCode();
            case 2 -> OrderStatusEnum.PAID.getCode();
            case 3 -> OrderStatusEnum.CANCELLED.getCode();
            case 4 -> OrderStatusEnum.REFUNDED.getCode();
            default -> adminStatus;
        };
    }
}
