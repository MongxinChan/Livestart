package com.mongxin.livestart.engine.mq.consumer;

import cn.hutool.core.lang.Singleton;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时关单消费者
 * <p>
 * 消费延时消息，将待支付的超时订单关闭，并归还 Redis 库存
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "livestart.engine.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_engine_order-delay-close_topic",
        consumerGroup = "livestart_engine_order-delay-close_cg"
)
@RequiredArgsConstructor
public class OrderDelayCloseConsumer implements RocketMQListener<String> {

    private static final String STOCK_ROLLBACK_LUA_PATH = "lua/stock_rollback.lua";

    private final OrderMapper orderMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @NoMQDuplicateConsume(keyPrefix = "engine:idempotent:mq:delay-close:", key = "#message")
    public void onMessage(String message) {
        log.info("[延时关单] 收到消息，message={}", message);

        MessageWrapper<OrderDelayCloseEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<OrderDelayCloseEvent>>() {});
        OrderDelayCloseEvent event = wrapper.getMessage();

        // 查询订单（按 orderNo 查找，携带 userId 走分片路由）
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, event.getOrderNo())
                .eq(OrderDO::getUserId, event.getUserId());
        OrderDO order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            log.warn("[延时关单] 订单不存在，orderNo={}", event.getOrderNo());
            return;
        }

        // 只有待支付状态才关闭
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.info("[延时关单] 订单状态已变更，跳过关单，orderNo={}, status={}", event.getOrderNo(), order.getStatus());
            return;
        }

        // CAS 更新订单状态为已取消
        int affected = orderMapper.updateOrderStatus(
                order.getId(),
                event.getUserId(),
                OrderStatusEnum.CANCELLED.getCode(),
                OrderStatusEnum.PENDING_PAYMENT.getCode()
        );
        if (affected <= 0) {
            log.warn("[延时关单] CAS 更新失败，orderNo={}", event.getOrderNo());
            return;
        }

        rollbackPreDeductStock(event);
        ticketSkuMapper.returnStock(event.getSkuId(), event.getCount());
    }

    private void rollbackPreDeductStock(OrderDelayCloseEvent event) {
        try {
            String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, event.getSkuId());
            String userLimitKey = String.format(
                    EngineRedisConstant.USER_TICKET_LIMIT_KEY,
                    event.getUserId(),
                    event.getEventId()
            );
            stringRedisTemplate.execute(
                    loadLongRedisScript(STOCK_ROLLBACK_LUA_PATH),
                    List.of(stockKey, userLimitKey),
                    String.valueOf(event.getCount()),
                    String.valueOf(event.getCount())
            );
            log.info("[延时关单] Redis 库存与限购计数已回滚，orderNo={}", event.getOrderNo());
        } catch (Exception ex) {
            log.error("[延时关单] Redis 回滚失败，orderNo={}", event.getOrderNo(), ex);
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
}
