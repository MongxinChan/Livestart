package com.mongxin.livestart.pay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.pay.common.PayStatus;
import com.mongxin.livestart.pay.config.AlipayProperties;
import com.mongxin.livestart.pay.config.PayServiceProperties;
import com.mongxin.livestart.pay.dao.entity.PayDO;
import com.mongxin.livestart.pay.dao.mapper.PayMapper;
import com.mongxin.livestart.pay.dto.PayCreateRequest;
import com.mongxin.livestart.pay.dto.PayCreateResponse;
import com.mongxin.livestart.pay.mq.PayResultEvent;
import com.mongxin.livestart.pay.mq.PayResultProducer;
import com.mongxin.livestart.pay.remote.OrderRemoteService;
import com.mongxin.livestart.pay.remote.dto.PayableOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {
    private final PayMapper payMapper;
    private final OrderRemoteService orderRemoteService;
    private final PayResultProducer payResultProducer;
    private final AlipayProperties alipayProperties;
    private final PayServiceProperties payServiceProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayCreateResponse create(PayCreateRequest request, Long userId) {
        PayDO existing = payMapper.selectOne(Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderNo, request.getOrderNo()));
        if (existing != null) {
            if (existing.getStatus() == PayStatus.TRADE_SUCCESS) {
                return PayCreateResponse.builder().paySn(existing.getPaySn()).orderNo(existing.getOrderNo())
                        .status(existing.getStatus()).build();
            }
            return buildAlipayForm(existing);
        }

        PayableOrderDTO order = orderRemoteService.getPayableOrder(
                request.getOrderNo(), userId, payServiceProperties.getInternalToken()).getData();
        if (order == null || order.getTotalAmount() == null) {
            throw new ClientException("订单不存在或不可支付");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ClientException("订单状态异常，无法发起支付");
        }

        PayDO pay = new PayDO();
        pay.setPaySn("P" + UUID.randomUUID().toString().replace("-", ""));
        pay.setOrderNo(order.getOrderNo());
        pay.setUserId(order.getUserId());
        pay.setSubject(request.getSubject() == null ? "LiveStart 演出门票" : request.getSubject());
        pay.setTotalAmount(order.getTotalAmount());
        pay.setStatus(PayStatus.WAIT_BUYER_PAY);
        pay.setCreateTime(new Date());
        pay.setUpdateTime(new Date());
        if (payMapper.insert(pay) <= 0) {
            throw new ServiceException("支付单创建失败");
        }
        return buildAlipayForm(pay);
    }

    private PayCreateResponse buildAlipayForm(PayDO pay) {
        try {
            AlipayClient client = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(), alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(), "json", alipayProperties.getCharset(),
                    alipayProperties.getPublicKey(), alipayProperties.getSignType());
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setReturnUrl(alipayProperties.getReturnUrl());
            request.setBizContent("{\"out_trade_no\":\"" + pay.getOrderNo()
                    + "\",\"total_amount\":\"" + pay.getTotalAmount().toPlainString()
                    + "\",\"subject\":\"" + pay.getSubject()
                    + "\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
            return PayCreateResponse.builder().paySn(pay.getPaySn()).orderNo(pay.getOrderNo())
                    .body(client.pageExecute(request).getBody()).status(pay.getStatus()).build();
        } catch (Exception e) {
            log.error("[支付创建] 支付宝调用失败，orderNo={}", pay.getOrderNo(), e);
            throw new ServiceException("支付宝支付接口调用失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callback(Map<String, String> params) {
        try {
            if (!AlipaySignature.rsaCheckV1(params, alipayProperties.getPublicKey(),
                    alipayProperties.getCharset(), alipayProperties.getSignType())) {
                throw new ClientException("支付宝回调验签失败");
            }
        } catch (Exception e) {
            throw new ClientException("支付宝回调验签失败");
        }
        if (!alipayProperties.getAppId().equals(params.get("app_id"))) {
            throw new ClientException("支付宝应用标识不匹配");
        }
        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        BigDecimal amount = new BigDecimal(params.get("total_amount"));
        PayDO pay = payMapper.selectOne(Wrappers.lambdaQuery(PayDO.class).eq(PayDO::getOrderNo, orderNo));
        if (pay == null || pay.getTotalAmount().compareTo(amount) != 0) {
            throw new ClientException("支付单不存在或金额不匹配");
        }
        String status = params.get("trade_status");
        if (!("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status))) {
            return;
        }
        if (pay.getStatus() == PayStatus.TRADE_SUCCESS) {
            return;
        }
        int affected = payMapper.markSuccessIfPending(orderNo, PayStatus.TRADE_SUCCESS,
                PayStatus.WAIT_BUYER_PAY, tradeNo, amount, new Date());
        if (affected == 1) {
            payResultProducer.send(PayResultEvent.builder().eventId(UUID.randomUUID().toString())
                    .paySn(pay.getPaySn()).orderNo(orderNo).userId(pay.getUserId()).tradeNo(tradeNo)
                    .payAmount(amount).paidAt(new Date()).build());
        }
    }
}
