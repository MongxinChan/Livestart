package com.mongxin.livestart.engine.mq.consumer;

import com.mongxin.livestart.framework.idempotent.NoMQDuplicateConsume;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderDelayCloseProducer;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.framework.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 璐エ寮傛涓嬪崟钀藉簱娑堣垂鑰咃紙楂樺苟鍙戝墛宄版牳蹇冪粍浠讹級
 * <p>
 * 娑堣垂 RocketMQ 涓殑涓嬪崟娑堟伅锛屽湪鍚庡彴绾跨▼姹犱腑骞虫粦骞跺彂鍦板皢璁㈠崟鏁版嵁鍐欏叆 MySQL 鏁版嵁搴擄紝
 * 鍐欏叆鎴愬姛鍚庡紑鍚欢鏃跺叧鍗曪紝澶辫触鍒欒嚜鍔ㄥ洖婊氬苟琛ュ伩 Redis 缂撳瓨搴撳瓨銆? */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "livestart.engine.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "livestart_engine_order-create_topic",
        consumerGroup = "livestart_engine_order-create_cg"
)
@RequiredArgsConstructor
public class TicketOrderCreateConsumer implements RocketMQListener<String> {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final MerchantAdminRemoteService merchantAdminRemoteService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderDelayCloseProducer orderDelayCloseProducer;

    /** 璁㈠崟瓒呮椂鍏冲崟寤舵椂锛?5鍒嗛挓锛屽崟浣?ms锛?*/
    private static final long ORDER_CLOSE_DELAY_MS = 15 * 60 * 1000L;

    @Override
    @NoMQDuplicateConsume(keyPrefix = "engine:idempotent:mq:create-order:", key = "#message")
    public void onMessage(String message) {
        log.info("[娑堣垂鑰匽 鏀跺埌寮傛涓嬪崟钀藉簱娑堟伅锛歿}", message);

        MessageWrapper<TicketOrderCreateEvent> wrapper = JSON.parseObject(
                message, new TypeReference<MessageWrapper<TicketOrderCreateEvent>>() {});
        TicketOrderCreateEvent event = wrapper.getMessage();

        // 1. 鏌ヨ鏈€鏂扮殑绁ㄧ SKU 淇℃伅
        TicketSkuDO sku = loadTicketSku(event.getSkuId());
        if (sku == null) {
            log.error("[娑堣垂鑰匽 寮傛涓嬪崟澶辫触锛氱エ绉?SKU 涓嶅瓨鍦紝skuId={}", event.getSkuId());
            compensateRedisStock(event);
            return;
        }

        try {
            // 2. 缂栫▼寮忎簨鍔″鐞嗭細涔愯閿佹墸 DB 搴撳瓨 + 鍐欏叆璁㈠崟 + 鍐欏叆鏄庣粏
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    int decremented = ticketSkuMapper.decrementStock(sku.getId(), event.getCount(), sku.getVersion());
                    if (!SqlHelper.retBool(decremented)) {
                        throw new ServiceException("?????????");
                    }

                    BigDecimal totalAmount = sku.getSellingPrice().multiply(BigDecimal.valueOf(event.getCount()));
                    Date now = new Date();
                    OrderDO order = OrderDO.builder()
                            .orderNo(event.getOrderNo())
                            .userId(event.getUserId())
                            .totalAmount(totalAmount)
                            .status(OrderStatusEnum.PENDING_PAYMENT.getCode())
                            .createTime(now)
                            .build();
                    orderMapper.insert(order);

                    // 鍐欒鍗曟槑缁嗭紙姣忓紶绁ㄤ竴鏉¤褰曪級
                    List<OrderItemDO> items = new ArrayList<>();
                    for (Long visitorId : event.getVisitorIds()) {
                        OrderItemDO item = OrderItemDO.builder()
                                .orderNo(event.getOrderNo())
                                .userId(event.getUserId())
                                .visitorId(visitorId)
                                .eventId(sku.getEventId())
                                .skuId(sku.getId())
                                .checkCode(generateCheckCode())
                                .isChecked(0)
                                .build();
                        items.add(item);
                    }
                    items.forEach(orderItemMapper::insert);

                    log.info("[娑堣垂鑰匽 寮傛涓嬪崟鏁版嵁搴撲簨鍔℃墽琛屾垚鍔燂紝orderNo={}", event.getOrderNo());
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    log.error("[娑堣垂鑰匽 寮傛涓嬪崟鏈湴浜嬪姟鎵ц寮傚父锛岃Е鍙戞暟鎹簱鍥炴粴锛宱rderNo={}", event.getOrderNo(), ex);
                    throw ex;
                }
            });

            // 3. 浜嬪姟鎴愬姛钀藉簱鍚庯紝鍙戦€佸欢鏃跺叧鍗曟秷鎭紙15鍒嗛挓鍚庢湭鏀粯鑷姩鍏冲崟锛?            sendDelayCloseMessage(event);

        } catch (Exception e) {
            // 4. 鍑虹幇寮傚父杩涜 Redis 缂撳瓨搴撳瓨鍥為€€琛ュ伩
            log.error("[娑堣垂鑰匽 寮傛涓嬪崟澶勭悊澶辫触锛屽紑濮嬫墽琛?Redis 搴撳瓨琛ュ伩锛宱rderNo={}", event.getOrderNo());
            compensateRedisStock(event);
        }
    }

    /**
     * 鍥為€€骞惰ˉ鍋?Redis 搴撳瓨
     */
    private void compensateRedisStock(TicketOrderCreateEvent event) {
        try {
            String stockKey = String.format(EngineRedisConstant.TICKET_STOCK_KEY, event.getSkuId());
            stringRedisTemplate.opsForValue().increment(stockKey, event.getCount());
            log.info("[娑堣垂鑰匽 Redis 搴撳瓨琛ュ伩鎴愬姛锛宻kuId={}锛屽洖閫€寮犳暟={}", event.getSkuId(), event.getCount());
        } catch (Exception ex) {
            log.error("[娑堣垂鑰匽 Redis 搴撳瓨琛ュ伩寮傚父锛堥潪闃诲锛夛紝skuId={}锛屽洖閫€寮犳暟={}", event.getSkuId(), event.getCount(), ex);
        }
    }

    /**
     * 鍙戦€?15 鍒嗛挓寤舵椂鍏冲崟娑堟伅
     */
    private void sendDelayCloseMessage(TicketOrderCreateEvent event) {
        long closeTime = System.currentTimeMillis() + ORDER_CLOSE_DELAY_MS;
        OrderDelayCloseEvent closeEvent = OrderDelayCloseEvent.builder()
                .orderNo(event.getOrderNo())
                .userId(event.getUserId())
                .skuId(event.getSkuId())
                .count(event.getCount())
                .delayTime(closeTime)
                .build();
        try {
            SendResult sendResult = orderDelayCloseProducer.sendMessage(closeEvent);
            if (!"SEND_OK".equals(sendResult.getSendStatus().name())) {
                log.warn("[娑堣垂鑰匽 寤舵椂鍏冲崟娑堟伅鍙戦€佸紓甯革紝杩斿洖鍊肩姸鎬侀潪 SEND_OK锛宱rderNo={}", event.getOrderNo());
            }
        } catch (Exception ex) {
            log.error("[娑堣垂鑰匽 寤舵椂鍏冲崟娑堟伅鍙戦€佸け璐ワ紝orderNo={}", event.getOrderNo(), ex);
        }
    }

    /**
     * 鐢熸垚鍞竴鏍搁攢鐮?     */
    private String generateCheckCode() {
        return UUID.fastUUID().toString(true).toUpperCase();
    }

    private TicketSkuDO loadTicketSku(Long skuId) {
        Result<MerchantTicketSkuDetailRespDTO> result = merchantAdminRemoteService.getTicketSku(skuId);
        if (result == null || result.isFail() || result.getData() == null) {
            log.warn("[绁ㄧ鏌ヨ] MQ 钀藉簱鍓嶈繙绋嬫煡璇㈢エ绉嶅け璐?| skuId={} | result={}", skuId, result);
            return null;
        }
        MerchantTicketSkuDetailRespDTO data = result.getData();
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
}

