package com.mongxin.livestart.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.engine.common.biz.user.UserContext;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.common.enums.StockDecrementErrorEnum;
import com.mongxin.livestart.engine.config.AlipayConfig;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.dto.req.AdminOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPayCallbackReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderRefundReqDTO;
import com.mongxin.livestart.engine.dto.resp.AdminOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderDetailRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.remote.AdminRemoteService;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.dto.AdminUserSimpleRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.framework.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketOrderServiceImpl implements TicketOrderService {

    private static final String STOCK_DECREMENT_LUA_PATH = "lua/stock_decrement.lua";
    private static final String STOCK_ROLLBACK_LUA_PATH = "lua/stock_rollback.lua";
    private static final long USER_LIMIT_KEY_EXPIRE_SECONDS = 7 * 24 * 3600L;
    private static final ConcurrentHashMap<Long, Boolean> SOLD_OUT_MAP = new ConcurrentHashMap<>();
    private static final String PATH_TOKEN_KEY = "engine:pathtoken:%s:%s";
    private static final String SECRET_SALT = "LiveStart_Engine_PathToken_Salt_Key";
    private static final int USER_TYPE_VENUE_ADMIN = 3;
    private static final int USER_TYPE_SUPER_ADMIN = 4;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final AdminRemoteService adminRemoteService;
    private final MerchantAdminRemoteService merchantAdminRemoteService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderPaySuccessProducer orderPaySuccessProducer;
    private final TicketOrderCreateProducer ticketOrderCreateProducer;
    private final AlipayConfig alipayConfig;
    private final JdbcTemplate jdbcTemplate;

    @Value("${livestart.engine.local-order-mode:true}")
    private boolean localOrderMode;

    @Value("${livestart.engine.auto-warm-stock-on-miss:true}")
    private boolean autoWarmStockOnMiss;

    @Override
    public String generatePathToken(Long skuId) {
        String userId = requireUserId();
        if (skuId == null) {
            throw new ClientException("票种不存在");
        }
        if (Boolean.TRUE.equals(SOLD_OUT_MAP.get(skuId))) {
            throw new ClientException("该票种已售罄");
        }

        TicketSkuDO sku = loadTicketSku(skuId);
        if (sku == null) {
            log.warn("[下单Token] 票种不存在，拒绝生成 token，userId={}, skuId={}", userId, skuId);
            throw new ClientException("票种不存在");
        }

        log.info("[下单Token] 票种校验通过，userId={}, skuId={}, eventId={}, remainingStock={}",
                userId, skuId, sku.getEventId(), sku.getRemainingStock());
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
        if (Boolean.TRUE.equals(SOLD_OUT_MAP.get(skuId))) {
            throw new ClientException("该票种已售罄");
        }

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
        if (sku.getRemainingStock() <= 0) {
            SOLD_OUT_MAP.put(skuId, true);
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
            if (errorCode == StockDecrementErrorEnum.STOCK_INSUFFICIENT.getCode()) {
                SOLD_OUT_MAP.put(sku.getId(), true);
            }
            throw new ServiceException(error.getMessage());
        }

        String orderNo = generateOrderNo(userId);
        TicketOrderCreateEvent createEvent = TicketOrderCreateEvent.builder()
                .orderNo(orderNo)
                .userId(userId)
                .skuId(sku.getId())
                .count(requestParam.getCount())
                .visitorIds(requestParam.getVisitorIds())
                .build();

        if (localOrderMode) {
            persistOrderDirectly(createEvent);
            log.info("[下单] 本地直写模式下单成功，userId={}, skuId={}, orderNo={}",
                    userId, requestParam.getSkuId(), orderNo);
            return orderNo;
        }

        SendResult sendResult = ticketOrderCreateProducer.sendMessage(createEvent);
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
    public void payCallback(TicketOrderPayCallbackReqDTO requestParam) {
        paySuccess(requestParam.getOrderNo(), requestParam.getTradeNo(), requestParam.getPayAmount());
    }

    @Override
    public String payWithAlipay(String orderNo) {
        String userId = requireUserId();
        OrderDO order = getOrderByNo(orderNo, Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (!OrderStatusEnum.PENDING_PAYMENT.equals(OrderStatusEnum.fromCode(order.getStatus()))) {
            throw new ClientException("订单状态异常，无法发起支付");
        }

        try {
            AlipayClient client = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getPrivateKey(),
                    "json",
                    alipayConfig.getCharset(),
                    alipayConfig.getPublicKey(),
                    alipayConfig.getSignType()
            );

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            request.setReturnUrl(alipayConfig.getReturnUrl());

            JSONObject biz = new JSONObject();
            biz.put("out_trade_no", order.getOrderNo());
            biz.put("total_amount", order.getTotalAmount().toString());
            biz.put("subject", "LiveStart 票务订单 - " + order.getOrderNo());
            biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(biz.toString());
            return client.pageExecute(request).getBody();
        } catch (Exception ex) {
            log.error("[支付宝支付] 发起支付异常，orderNo={}", orderNo, ex);
            throw new ServiceException("支付宝支付接口调用失败：" + ex.getMessage());
        }
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
            log.info("[支付成功通知] 订单已支付，忽略重复通知，orderNo={}", orderNo);
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

        OrderPaySuccessEvent payEvent = OrderPaySuccessEvent.builder()
                .orderNo(orderNo)
                .userId(order.getUserId())
                .tradeNo(tradeNo)
                .build();
        if (localOrderMode) {
            log.info("[支付成功通知] 本地直写模式已处理支付成功，跳过 RocketMQ 投递，orderNo={}", orderNo);
            return;
        }

        SendResult sendResult = orderPaySuccessProducer.sendMessage(payEvent);
        if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
            log.warn("[支付成功通知] 支付成功消息发送失败，orderNo={}", orderNo);
        }

        log.info("[支付成功通知] 支付处理成功并已投递出票事件，orderNo={}", orderNo);
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
        Long skuId = CollUtil.isNotEmpty(items) ? items.get(0).getSkuId() : null;

        int affected = orderMapper.updateOrderStatus(
                order.getId(),
                order.getUserId(),
                OrderStatusEnum.CANCELLED.getCode(),
                OrderStatusEnum.PENDING_PAYMENT.getCode()
        );
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("取消订单失败，请重试");
        }

        Long eventId = CollUtil.isNotEmpty(items) ? items.get(0).getEventId() : null;
        restoreStockIfNeeded(skuId, count, order.getUserId(), eventId);
        log.info("[取消订单] 订单已取消，orderNo={}", requestParam.getOrderNo());
    }

    @Override
    public void refundOrder(TicketOrderRefundReqDTO requestParam) {
        String userId = requireUserId();
        OrderDO order = getOrderByNo(requestParam.getOrderNo(), Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PAID.getCode()) {
            throw new ClientException("仅已支付订单可以申请退票");
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

        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, requestParam.getOrderNo())
                .eq(OrderItemDO::getUserId, order.getUserId()));
        if (CollUtil.isNotEmpty(items)) {
            restoreStockIfNeeded(items.get(0).getSkuId(), items.size(),
                    order.getUserId(), items.get(0).getEventId());
        }

        log.info("[退票] 退票成功，orderNo={}", requestParam.getOrderNo());
    }

    @Override
    public IPage<TicketOrderPageQueryRespDTO> pageQueryOrders(TicketOrderPageQueryReqDTO requestParam) {
        String userId = requireUserId();
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, Long.parseLong(userId))
                .eq(requestParam.getStatus() != null, OrderDO::getStatus, requestParam.getStatus())
                .orderByDesc(OrderDO::getCreateTime);
        IPage<OrderDO> page = orderMapper.selectPage(new Page<>(requestParam.getCurrent(), requestParam.getSize()), queryWrapper);
        return page.convert(order -> {
            TicketOrderPageQueryRespDTO dto = new TicketOrderPageQueryRespDTO();
            dto.setOrderNo(order.getOrderNo());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setStatus(order.getStatus());
            dto.setStatusDesc(OrderStatusEnum.fromCode(order.getStatus()).getDesc());
            dto.setCreateTime(order.getCreateTime());
            return dto;
        });
    }

    @Override
    public IPage<AdminOrderPageQueryRespDTO> pageQueryAdminOrders(AdminOrderPageQueryReqDTO requestParam) {
        Long currentUserId = Long.parseLong(requireUserId());
        Integer userType = UserContext.getUserType();
        if (userType == null || (userType != USER_TYPE_SUPER_ADMIN && userType != USER_TYPE_VENUE_ADMIN)) {
            throw new ClientException("当前用户无后台订单查看权限");
        }

        StringBuilder fromSql = new StringBuilder("""
                FROM t_order o
                INNER JOIN (
                    SELECT order_no,
                           user_id,
                           MIN(event_id) AS event_id,
                           MIN(sku_id) AS sku_id,
                           COUNT(*) AS ticket_count
                    FROM t_order_item
                    GROUP BY order_no, user_id
                ) oi ON oi.order_no = o.order_no AND oi.user_id = o.user_id
                INNER JOIN t_event e ON e.id = oi.event_id
                INNER JOIN t_ticket_sku sku ON sku.id = oi.sku_id
                INNER JOIN t_venue v ON v.id = e.venue_id
                WHERE 1 = 1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (requestParam.getStatus() != null) {
            fromSql.append(" AND o.status = ?");
            params.add(toOrderStatusCode(requestParam.getStatus()));
        }
        if (userType == USER_TYPE_VENUE_ADMIN) {
            fromSql.append(" AND v.owner_user_id = ?");
            params.add(currentUserId);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT o.order_no " + fromSql + " GROUP BY o.order_no, o.user_id) t",
                Long.class,
                params.toArray()
        );

        long current = requestParam.getCurrent() <= 0 ? 1 : requestParam.getCurrent();
        long size = requestParam.getSize() <= 0 ? 10 : requestParam.getSize();
        long offset = (current - 1) * size;

        String querySql = """
                SELECT o.order_no,
                       o.user_id,
                       oi.event_id,
                       e.title AS event_title,
                       oi.sku_id,
                       sku.title AS sku_name,
                       oi.ticket_count,
                       o.total_amount,
                       o.status,
                       o.create_time
                """ + fromSql + """
                GROUP BY o.order_no, o.user_id, oi.event_id, e.title, oi.sku_id, sku.title, oi.ticket_count, o.total_amount, o.status, o.create_time
                ORDER BY o.create_time DESC
                LIMIT ? OFFSET ?
                """;
        params.add(size);
        params.add(offset);

        List<AdminOrderPageQueryRespDTO> records = jdbcTemplate.query(
                querySql,
                (rs, rowNum) -> mapAdminOrderRow(rs),
                params.toArray()
        );
        fillUsernames(records);

        Page<AdminOrderPageQueryRespDTO> page = new Page<>(current, size, total == null ? 0 : total);
        page.setRecords(records);
        return page;
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

    private void restoreStockIfNeeded(Long skuId, int count, Long userId, Long eventId) {
        if (skuId == null || count <= 0) {
            return;
        }
        String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, skuId);
        if (userId != null && eventId != null) {
            String userLimitKey = String.format(EngineRedisConstant.USER_TICKET_LIMIT_KEY, userId, eventId);
            rollbackPreDeductStock(stockKey, userLimitKey, count);
        } else {
            try {
                stringRedisTemplate.opsForValue().increment(stockKey, count);
            } catch (Exception e) {
                log.error("[库存回补] Redis 库存回补失败，skuId={}", skuId, e);
            }
        }
        if (!localOrderMode) {
            ticketSkuMapper.returnStock(skuId, count);
        } else {
            log.warn("[库存回补] local-order-mode 已启用，跳过数据库库存回补，skuId={}, count={}", skuId, count);
        }
        SOLD_OUT_MAP.remove(skuId);
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
        transactionTemplate.executeWithoutResult(status -> {
            try {
                TicketSkuDO latestSku = loadTicketSku(event.getSkuId());
                if (!localOrderMode) {
                    int decremented = ticketSkuMapper.decrementStock(latestSku.getId(), event.getCount(), latestSku.getVersion());
                    if (!SqlHelper.retBool(decremented)) {
                        throw new ServiceException("库存扣减失败");
                    }
                } else {
                    log.warn("[下单] local-order-mode 已启用，跳过数据库库存扣减，依赖 Redis 预扣库存，skuId={}, count={}",
                            latestSku.getId(), event.getCount());
                }

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
                            .checkCode(UUID.fastUUID().toString(true).toUpperCase())
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

    private OrderDO getOrderByOrderNo(String orderNo) {
        return orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, orderNo));
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

    private AdminOrderPageQueryRespDTO mapAdminOrderRow(ResultSet rs) throws java.sql.SQLException {
        AdminOrderPageQueryRespDTO dto = new AdminOrderPageQueryRespDTO();
        dto.setOrderNo(rs.getString("order_no"));
        dto.setUserId(rs.getLong("user_id"));
        dto.setEventId(rs.getLong("event_id"));
        dto.setEventTitle(rs.getString("event_title"));
        dto.setSkuId(rs.getLong("sku_id"));
        dto.setSkuName(rs.getString("sku_name"));
        dto.setTicketCount(rs.getInt("ticket_count"));
        dto.setTotalAmount(rs.getBigDecimal("total_amount"));
        dto.setStatus(rs.getInt("status"));
        dto.setStatusDesc(resolveStatusDesc(rs.getInt("status")));
        dto.setCreateTime(rs.getTimestamp("create_time"));
        return dto;
    }

    private void fillUsernames(List<AdminOrderPageQueryRespDTO> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        List<Long> userIds = records.stream().map(AdminOrderPageQueryRespDTO::getUserId).distinct().toList();
        Result<List<AdminUserSimpleRespDTO>> result = adminRemoteService.listSimpleUsersByIds(userIds);
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
