package com.mongxin.livestart.pay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.pay.common.PayStatus;
import com.mongxin.livestart.pay.config.AlipayProperties;
import com.mongxin.livestart.pay.dao.entity.PayDO;
import com.mongxin.livestart.pay.dao.entity.RefundDO;
import com.mongxin.livestart.pay.dao.mapper.PayMapper;
import com.mongxin.livestart.pay.dao.mapper.RefundMapper;
import com.mongxin.livestart.pay.dto.RefundCreateRequest;
import com.mongxin.livestart.pay.dto.RefundCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private static final int REFUND_SUCCESS = 1;
    private static final String ALIPAY_REFUND_SUCCESS = "REFUND_SUCCESS";
    private final PayMapper payMapper;
    private final RefundMapper refundMapper;
    private final AlipayProperties alipayProperties;

    @Override
    public RefundCreateResponse query(String orderNo) {
        RefundDO refund = refundMapper.selectOne(Wrappers.lambdaQuery(RefundDO.class)
                .eq(RefundDO::getOrderNo, orderNo));
        return refund == null ? null : buildResponse(refund);
    }

    @Override
    public void reconcilePendingRefunds() {
        List<RefundDO> pending = refundMapper.selectPending();
        for (RefundDO refund : pending) {
            try {
                reconcileOne(refund);
            } catch (Exception ex) {
                log.error("[退款对账] 查询支付宝退款结果失败，orderNo={}, refundNo={}",
                        refund.getOrderNo(), refund.getRefundNo(), ex);
            }
        }
    }

    private void reconcileOne(RefundDO refund) throws Exception {
        AlipayClient client = new DefaultAlipayClient(
                alipayProperties.getGatewayUrl(), alipayProperties.getAppId(),
                alipayProperties.getPrivateKey(), "json", alipayProperties.getCharset(),
                alipayProperties.getPublicKey(), alipayProperties.getSignType());
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setTradeNo(refund.getTradeNo());
        model.setOutRequestNo(refund.getRefundNo());
        request.setBizModel(model);
        AlipayTradeFastpayRefundQueryResponse response = client.execute(request);
        if (response != null && response.isSuccess()
                && ALIPAY_REFUND_SUCCESS.equalsIgnoreCase(response.getRefundStatus())) {
            int affected = refundMapper.markSuccessIfPending(refund.getOrderNo(), refund.getRefundNo(),
                    REFUND_SUCCESS, 0, response.getTradeNo(), new Date());
            if (affected == 1) {
                log.info("[退款对账] 已修正退款成功状态，orderNo={}, refundNo={}",
                        refund.getOrderNo(), refund.getRefundNo());
            }
        }
    }

    @Override
    public RefundCreateResponse create(RefundCreateRequest request, Long userId) {
        PayDO pay = payMapper.selectOne(Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderNo, request.getOrderNo())
                .eq(PayDO::getUserId, userId));
        if (pay == null || pay.getStatus() != PayStatus.TRADE_SUCCESS || pay.getTradeNo() == null) {
            throw new ClientException("支付单不存在或未支付");
        }
        RefundDO existing = refundMapper.selectOne(Wrappers.lambdaQuery(RefundDO.class)
                .eq(RefundDO::getOrderNo, request.getOrderNo()));
        if (existing != null) {
            validateSameRefund(existing, request.getRefundAmount());
            if (existing.getStatus() == REFUND_SUCCESS) {
                return buildResponse(existing);
            }
            return executeRefund(existing, pay);
        }

        RefundDO refund = new RefundDO();
        refund.setRefundNo("R" + UUID.randomUUID().toString().replace("-", ""));
        refund.setOrderNo(pay.getOrderNo());
        refund.setPaySn(pay.getPaySn());
        refund.setTradeNo(pay.getTradeNo());
        BigDecimal paidAmount = pay.getPayAmount() == null ? pay.getTotalAmount() : pay.getPayAmount();
        BigDecimal refundAmount = request.getRefundAmount().setScale(2, RoundingMode.UNNECESSARY);
        if (refundAmount.signum() <= 0 || refundAmount.compareTo(paidAmount) > 0) {
            throw new ClientException("退款金额不合法");
        }
        refund.setRefundAmount(refundAmount);
        refund.setReason(request.getReason());
        refund.setStatus(0);
        refund.setCreateTime(new Date());
        refund.setUpdateTime(new Date());
        if (refundMapper.insert(refund) <= 0) {
            throw new ServiceException("退款单创建失败");
        }

        return executeRefund(refund, pay);
    }

    private RefundCreateResponse executeRefund(RefundDO refund, PayDO pay) {
        try {
            AlipayClient client = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(), alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(), "json", alipayProperties.getCharset(),
                    alipayProperties.getPublicKey(), alipayProperties.getSignType());
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setTradeNo(pay.getTradeNo());
            model.setOutTradeNo(pay.getOrderNo());
            model.setRefundAmount(refund.getRefundAmount().toPlainString());
            model.setOutRequestNo(refund.getRefundNo());
            AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
            refundRequest.setBizModel(model);
            AlipayTradeRefundResponse response = client.execute(refundRequest);
            if (!response.isSuccess()) {
                throw new ServiceException("支付宝退款失败：" + response.getSubMsg());
            }
            refund.setStatus(REFUND_SUCCESS);
            refund.setRefundTradeNo(response.getTradeNo());
            refund.setUpdateTime(new Date());
            int affected = refundMapper.markSuccessIfPending(refund.getOrderNo(), refund.getRefundNo(),
                    REFUND_SUCCESS, 0, refund.getRefundTradeNo(), refund.getUpdateTime());
            if (affected != 1) {
                RefundDO latest = refundMapper.selectOne(Wrappers.lambdaQuery(RefundDO.class)
                        .eq(RefundDO::getOrderNo, refund.getOrderNo()));
                if (latest == null || latest.getStatus() != REFUND_SUCCESS) {
                    throw new ServiceException("退款结果保存失败");
                }
                return buildResponse(latest);
            }
            return buildResponse(refund);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[退款] 支付宝退款调用失败，orderNo={}", refund.getOrderNo(), e);
            throw new ServiceException("支付宝退款接口调用失败");
        }
    }

    private void validateSameRefund(RefundDO existing, BigDecimal requestedAmount) {
        if (requestedAmount == null || existing.getRefundAmount().compareTo(requestedAmount) != 0) {
            throw new ClientException("该订单已存在不同金额的退款申请");
        }
    }

    private RefundCreateResponse buildResponse(RefundDO refund) {
        return RefundCreateResponse.builder().refundNo(refund.getRefundNo())
                .orderNo(refund.getOrderNo()).status(refund.getStatus()).build();
    }
}
