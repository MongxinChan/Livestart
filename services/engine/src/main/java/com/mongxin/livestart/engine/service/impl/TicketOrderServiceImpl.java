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
import com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPayCallbackReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderRefundReqDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderDetailRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderPaySuccessProducer orderPaySuccessProducer;
    private final TicketOrderCreateProducer ticketOrderCreateProducer;
    private final AlipayConfig alipayConfig;

    @Override
    public String generatePathToken(Long skuId) {
        String userId = requireUserId();
        if (Boolean.TRUE.equals(SOLD_OUT_MAP.get(skuId))) {
            throw new ClientException("该票种已售罄");
        }

        String tokenSource = userId + "_" + skuId + "_" + SECRET_SALT + "_" + UUID.fastUUID().toString(true);
        String pathToken = SecureUtil.md5(tokenSource);
        String tokenKey = String.format(PATH_TOKEN_KEY, userId, skuId);
        stringRedisTemplate.opsForValue().set(tokenKey, pathToken, 5, java.util.concurrent.TimeUnit.SECONDS);
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

        TicketSkuDO sku = ticketSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new ClientException("票种不存在");
        }
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
            log.warn("[支付成功通知] 订单状态不允许支付成功流转，orderNo={}, status={}",
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
                    throw new ServiceException("支付通知处理失败（状态已变更）");
                }
                OrderDO updateOrder = OrderDO.builder()
                        .id(order.getId())
                        .userId(order.getUserId())
                        .payTime(new Date())
                        .build();
                orderMapper.updateById(updateOrder);
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
            throw new ClientException("观演人数量与购买数量不符");
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
        ticketSkuMapper.returnStock(skuId, count);
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
}
