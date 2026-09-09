package com.mongxin.livestart.engine.service;

import com.mongxin.livestart.engine.config.RefundPolicyProperties;
import com.mongxin.livestart.engine.dao.entity.RefundPolicySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class RefundPolicyEvaluator {
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final RefundPolicyProperties properties;

    public RefundDecision evaluate(RefundPolicySnapshot snapshot,
                                   BigDecimal paidAmount,
                                   Instant requestedAt) {
        if (snapshot == null || snapshot.getEventStartTime() == null) {
            return RefundDecision.reject("演出退票策略或演出时间不存在");
        }
        if (requestedAt == null) {
            return RefundDecision.reject("退款申请时间不存在");
        }
        if (!Integer.valueOf(1).equals(snapshot.getAllowRefund())) {
            return RefundDecision.reject("该演出不允许退票");
        }
        if (paidAmount == null || paidAmount.signum() <= 0) {
            return RefundDecision.reject("退款金额必须大于 0");
        }

        Instant eventStart = snapshot.getEventStartTime().toInstant();
        if (!eventStart.isAfter(requestedAt)) {
            return RefundDecision.reject("演出已开始，不能退票");
        }
        long minutesBeforeStart = Duration.between(requestedAt, eventStart).toMinutes();

        int tier1Hours = positiveOrDefault(snapshot.getTier1DeadlineHours(), properties.getDefaultTier1DeadlineHours());
        int tier2Hours = positiveOrDefault(snapshot.getTier2DeadlineHours(), properties.getDefaultTier2DeadlineHours());
        BigDecimal feeRate = validFeeRate(snapshot.getTier2RefundFeeRate())
                ? snapshot.getTier2RefundFeeRate() : properties.getDefaultTier2RefundFeeRate();
        if (tier1Hours < tier2Hours) {
            return RefundDecision.reject("退票策略配置错误：全额截止时间不能早于部分退款截止时间");
        }

        BigDecimal refundAmount;
        String tier;
        if (minutesBeforeStart >= tier1Hours * 60L) {
            refundAmount = paidAmount;
            tier = "FULL";
        } else if (minutesBeforeStart >= tier2Hours * 60L) {
            refundAmount = paidAmount.multiply(ONE.subtract(feeRate))
                    .setScale(2, RoundingMode.DOWN);
            tier = "PARTIAL";
        } else {
            return RefundDecision.reject("已超过退票截止时间");
        }
        if (refundAmount.signum() <= 0) {
            return RefundDecision.reject("按退票策略计算后退款金额为 0");
        }
        return RefundDecision.allow(refundAmount, tier, minutesBeforeStart);
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private boolean validFeeRate(BigDecimal feeRate) {
        return feeRate != null && feeRate.compareTo(BigDecimal.ZERO) >= 0
                && feeRate.compareTo(ONE) < 0;
    }

    public record RefundDecision(boolean allowed, BigDecimal refundAmount, String tier,
                                 long minutesBeforeStart, String rejectionReason) {
        public static RefundDecision allow(BigDecimal amount, String tier, long minutesBeforeStart) {
            return new RefundDecision(true, amount, tier, minutesBeforeStart, null);
        }

        public static RefundDecision reject(String reason) {
            return new RefundDecision(false, null, null, 0, reason);
        }
    }
}
