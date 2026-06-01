package com.mongxin.livestart.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
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
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderDelayCloseProducer;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购票订单服务实现层（核心主链路）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketOrderServiceImpl implements TicketOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final OrderDelayCloseProducer orderDelayCloseProducer;
    private final OrderPaySuccessProducer orderPaySuccessProducer;
    private final TicketOrderCreateProducer ticketOrderCreateProducer;

    private static final String STOCK_DECREMENT_LUA_PATH = "lua/stock_decrement.lua";
    /** 订单超时关单延时（15分钟，单位 ms） */
    private static final long ORDER_CLOSE_DELAY_MS = 15 * 60 * 1000L;
    /** 用户购票限额 Key 过期时间（7天，演出结束后仍可查询） */
    private static final long USER_LIMIT_KEY_EXPIRE_SECONDS = 7 * 24 * 3600L;

    // ============================== 1. 购票下单 ==============================

    @Override
    public String createOrder(TicketOrderCreateReqDTO requestParam) {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户未登录");
        }

        // 参数基础校验：购买数量与观演人数量一致
        if (CollUtil.isEmpty(requestParam.getVisitorIds())
                || requestParam.getVisitorIds().size() != requestParam.getCount()) {
            throw new ClientException("观演人数量与购买数量不符");
        }

        // 查询票种信息（公共库，ShardingSphere defaultDataSource）
        TicketSkuDO sku = ticketSkuMapper.selectById(requestParam.getSkuId());
        if (sku == null) {
            throw new ClientException("票种不存在");
        }
        if (sku.getRemainingStock() <= 0) {
            throw new ClientException("票种库存不足");
        }

        return doCreateOrder(requestParam, sku, Long.parseLong(userId));
    }

    private String doCreateOrder(TicketOrderCreateReqDTO requestParam, TicketSkuDO sku, Long userId) {
        // Redis Lua 原子扣减库存 + 校验个人限额
        DefaultRedisScript<Long> luaScript = Singleton.get(STOCK_DECREMENT_LUA_PATH, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_LUA_PATH)));
            script.setResultType(Long.class);
            return script;
        });

        String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, sku.getId());
        String userLimitKey = String.format(EngineRedisConstant.USER_TICKET_LIMIT_KEY, userId, sku.getEventId());
        int maxLimit = sku.getLimitNum() != null ? sku.getLimitNum() : 6;

        Long luaResult = stringRedisTemplate.execute(
                luaScript,
                List.of(stockKey, userLimitKey),
                String.valueOf(requestParam.getCount()),
                String.valueOf(maxLimit),
                String.valueOf(USER_LIMIT_KEY_EXPIRE_SECONDS)
        );

        long errorCode = StockDecrementReturnCombinedUtil.extractErrorCode(luaResult);
        if (StockDecrementErrorEnum.isFail(errorCode)) {
            StockDecrementErrorEnum error = StockDecrementErrorEnum.fromCode(errorCode);
            throw new ServiceException(error.getMessage());
        }

        // 生成订单号（时间戳 + userId 后4位 + 随机4位）
        String orderNo = generateOrderNo(userId);

        // 组装异步下单事件
        TicketOrderCreateEvent createEvent = TicketOrderCreateEvent.builder()
                .orderNo(orderNo)
                .userId(userId)
                .skuId(sku.getId())
                .count(requestParam.getCount())
                .visitorIds(requestParam.getVisitorIds())
                .build();

        // 投递异步下单消息到 RocketMQ（高并发削峰）
        SendResult sendResult = ticketOrderCreateProducer.sendMessage(createEvent);
        if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
            log.error("[下单] 异步下单消息投递失败，触发本地补偿，orderNo={}", orderNo);
            // 归还已扣减的 Redis 缓存库存
            try {
                stringRedisTemplate.opsForValue().increment(stockKey, requestParam.getCount());
            } catch (Exception redisEx) {
                log.error("[下单] Redis 库存补偿失败（非阻塞），skuId={}，count={}", sku.getId(), requestParam.getCount(), redisEx);
            }
            throw new ServiceException("抢票排队人数较多，请稍后重试");
        }

        log.info("[下单] 购票异步下单投递成功，进入后台落库排队，userId={}，skuId={}，orderNo={}", userId, requestParam.getSkuId(), orderNo);
        return orderNo;
    }

    // ============================== 2. 支付回调 ==============================

    @Override
    public void payCallback(TicketOrderPayCallbackReqDTO requestParam) {
        String userId = UserContext.getUserId();

        // 查询订单（按 orderNo + userId 走分片路由）
        OrderDO order = getOrderByNo(requestParam.getOrderNo(), Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            throw new ClientException("订单状态异常，无法完成支付");
        }

        // CAS 更新为已支付
        transactionTemplate.executeWithoutResult(status -> {
            try {
                int affected = orderMapper.updateOrderStatus(
                        order.getId(), order.getUserId(),
                        OrderStatusEnum.PAID.getCode(),
                        OrderStatusEnum.PENDING_PAYMENT.getCode()
                );
                if (!SqlHelper.retBool(affected)) {
                    throw new ServiceException("支付回调处理失败（状态冲突）");
                }
                // 更新支付时间（简化处理，直接 update）
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

        // 发送支付成功消息（出票后置处理）
        OrderPaySuccessEvent payEvent = OrderPaySuccessEvent.builder()
                .orderNo(requestParam.getOrderNo())
                .userId(order.getUserId())
                .tradeNo(requestParam.getTradeNo())
                .build();
        SendResult sendResult = orderPaySuccessProducer.sendMessage(payEvent);
        if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
            log.warn("[支付回调] 支付成功消息发送失败，orderNo={}", requestParam.getOrderNo());
        }

        log.info("[支付回调] 支付成功出票，orderNo={}", requestParam.getOrderNo());
    }

    // ============================== 3. 取消订单 ==============================

    @Override
    public void cancelOrder(TicketOrderCancelReqDTO requestParam) {
        String userId = UserContext.getUserId();
        OrderDO order = getOrderByNo(requestParam.getOrderNo(), Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            throw new ClientException("仅待支付订单可以取消");
        }

        // 查询订单明细获取 skuId 和数量
        LambdaQueryWrapper<OrderItemDO> itemQuery = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, requestParam.getOrderNo())
                .eq(OrderItemDO::getUserId, order.getUserId());
        List<OrderItemDO> items = orderItemMapper.selectList(itemQuery);
        int count = items.size();
        Long skuId = CollUtil.isNotEmpty(items) ? items.get(0).getSkuId() : null;

        // CAS 更新订单状态为已取消
        int affected = orderMapper.updateOrderStatus(
                order.getId(), order.getUserId(),
                OrderStatusEnum.CANCELLED.getCode(),
                OrderStatusEnum.PENDING_PAYMENT.getCode()
        );
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("取消订单失败，请重试");
        }

        // 归还 Redis + DB 库存
        if (skuId != null && count > 0) {
            try {
                String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, skuId);
                stringRedisTemplate.opsForValue().increment(stockKey, count);
            } catch (Exception e) {
                log.error("[取消] Redis库存归还失败，skuId={}", skuId, e);
            }
            ticketSkuMapper.returnStock(skuId, count);
        }

        log.info("[取消] 订单已取消，orderNo={}", requestParam.getOrderNo());
    }

    // ============================== 4. 退票 ==============================

    @Override
    public void refundOrder(TicketOrderRefundReqDTO requestParam) {
        String userId = UserContext.getUserId();
        OrderDO order = getOrderByNo(requestParam.getOrderNo(), Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PAID.getCode()) {
            throw new ClientException("仅已支付订单可以申请退票");
        }

        // CAS 更新订单状态为已退票
        int affected = orderMapper.updateOrderStatus(
                order.getId(), order.getUserId(),
                OrderStatusEnum.REFUNDED.getCode(),
                OrderStatusEnum.PAID.getCode()
        );
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("退票申请失败，请重试");
        }

        // 归还库存
        LambdaQueryWrapper<OrderItemDO> itemQuery = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, requestParam.getOrderNo())
                .eq(OrderItemDO::getUserId, order.getUserId());
        List<OrderItemDO> items = orderItemMapper.selectList(itemQuery);
        if (CollUtil.isNotEmpty(items)) {
            Long skuId = items.get(0).getSkuId();
            int count = items.size();
            try {
                String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, skuId);
                stringRedisTemplate.opsForValue().increment(stockKey, count);
            } catch (Exception e) {
                log.error("[退票] Redis库存归还失败，skuId={}", skuId, e);
            }
            ticketSkuMapper.returnStock(skuId, count);
        }

        log.info("[退票] 退票成功，orderNo={}", requestParam.getOrderNo());
    }

    // ============================== 5. 我的订单分页查询 ==============================

    @Override
    public IPage<TicketOrderPageQueryRespDTO> pageQueryOrders(TicketOrderPageQueryReqDTO requestParam) {
        String userId = UserContext.getUserId();
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, Long.parseLong(userId))
                .eq(requestParam.getStatus() != null, OrderDO::getStatus, requestParam.getStatus())
                .orderByDesc(OrderDO::getCreateTime);

        IPage<OrderDO> queryPage = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<OrderDO> page = orderMapper.selectPage(queryPage, queryWrapper);
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

    // ============================== 6. 订单详情 ==============================

    @Override
    public TicketOrderDetailRespDTO getOrderDetail(String orderNo) {
        String userId = UserContext.getUserId();
        OrderDO order = getOrderByNo(orderNo, Long.parseLong(userId));
        if (order == null) {
            throw new ClientException("订单不存在");
        }

        LambdaQueryWrapper<OrderItemDO> itemQuery = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderNo, orderNo)
                .eq(OrderItemDO::getUserId, order.getUserId());
        List<OrderItemDO> items = orderItemMapper.selectList(itemQuery);

        TicketOrderDetailRespDTO dto = new TicketOrderDetailRespDTO();
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusDesc(OrderStatusEnum.fromCode(order.getStatus()).getDesc());
        dto.setPayTime(order.getPayTime());
        dto.setCreateTime(order.getCreateTime());

        List<TicketOrderDetailRespDTO.TicketItemRespDTO> ticketItems = items.stream().map(item -> {
            TicketOrderDetailRespDTO.TicketItemRespDTO ticketItem = new TicketOrderDetailRespDTO.TicketItemRespDTO();
            ticketItem.setId(item.getId());
            ticketItem.setVisitorId(item.getVisitorId());
            ticketItem.setCheckCode(item.getCheckCode());
            ticketItem.setIsChecked(item.getIsChecked());
            return ticketItem;
        }).collect(Collectors.toList());
        dto.setTicketItems(ticketItems);

        return dto;
    }

    // ============================== 私有工具方法 ==============================

    private OrderDO getOrderByNo(String orderNo, Long userId) {
        LambdaQueryWrapper<OrderDO> query = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, orderNo)
                .eq(OrderDO::getUserId, userId);
        return orderMapper.selectOne(query);
    }

    /**
     * 生成可读订单流水号（20位）
     */
    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userSuffix = String.format("%04d", userId % 10000);
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return timestamp + userSuffix + randomSuffix;
    }

    /**
     * 生成唯一核销码（UUID 去横线，32位）
     */
    private String generateCheckCode(String orderNo, Long visitorId) {
        return UUID.fastUUID().toString(true).toUpperCase();
    }
}
