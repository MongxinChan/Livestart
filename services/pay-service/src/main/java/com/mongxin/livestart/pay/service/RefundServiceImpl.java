package com.mongxin.livestart.pay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private static final int REFUND_SUCCESS = 1;
    private final PayMapper payMapper;
    private final RefundMapper refundMapper;
    private final AlipayProperties alipayProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            return RefundCreateResponse.builder().refundNo(existing.getRefundNo())
                    .orderNo(existing.getOrderNo()).status(existing.getStatus()).build();
        }

        RefundDO refund = new RefundDO();
        refund.setRefundNo("R" + UUID.randomUUID().toString().replace("-", ""));
        refund.setOrderNo(pay.getOrderNo());
        refund.setPaySn(pay.getPaySn());
        refund.setTradeNo(pay.getTradeNo());
        refund.setRefundAmount(pay.getTotalAmount());
        refund.setReason(request.getReason());
        refund.setStatus(0);
        refund.setCreateTime(new Date());
        refund.setUpdateTime(new Date());
        if (refundMapper.insert(refund) <= 0) {
            throw new ServiceException("退款单创建失败");
        }

        try {
            AlipayClient client = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(), alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(), "json", alipayProperties.getCharset(),
                    alipayProperties.getPublicKey(), alipayProperties.getSignType());
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setTradeNo(pay.getTradeNo());
            model.setOutTradeNo(pay.getOrderNo());
            model.setRefundAmount(pay.getTotalAmount().toPlainString());
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
            refundMapper.updateById(refund);
            return RefundCreateResponse.builder().refundNo(refund.getRefundNo())
                    .orderNo(refund.getOrderNo()).status(refund.getStatus()).build();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[退款] 支付宝退款调用失败，orderNo={}", request.getOrderNo(), e);
            throw new ServiceException("支付宝退款接口调用失败");
        }
    }
}
