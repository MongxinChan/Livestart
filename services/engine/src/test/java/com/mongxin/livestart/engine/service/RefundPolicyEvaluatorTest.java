package com.mongxin.livestart.engine.service;

import com.mongxin.livestart.engine.config.RefundPolicyProperties;
import com.mongxin.livestart.engine.dao.entity.RefundPolicySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundPolicyEvaluatorTest {
    private static final Instant REQUESTED_AT = Instant.parse("2026-10-01T10:00:00Z");
    private RefundPolicyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        RefundPolicyProperties properties = new RefundPolicyProperties();
        properties.setDefaultTier1DeadlineHours(48);
        properties.setDefaultTier2DeadlineHours(24);
        properties.setDefaultTier2RefundFeeRate(new BigDecimal("0.20"));
        evaluator = new RefundPolicyEvaluator(properties);
    }

    @Test
    void shouldRefundFullAmountAtTier1Boundary() {
        var decision = evaluator.evaluate(policy(48, null, null, null),
                new BigDecimal("100.00"), REQUESTED_AT);
        assertTrue(decision.allowed());
        assertEquals("FULL", decision.tier());
        assertEquals(new BigDecimal("100.00"), decision.refundAmount());
    }

    @Test
    void shouldUseDefaultsAndDeductFeeInsidePartialWindow() {
        var decision = evaluator.evaluate(policy(30, null, null, null),
                new BigDecimal("199.00"), REQUESTED_AT);
        assertTrue(decision.allowed());
        assertEquals("PARTIAL", decision.tier());
        assertEquals(new BigDecimal("159.20"), decision.refundAmount());
    }

    @Test
    void shouldAllowPartialRefundAtTier2Boundary() {
        var decision = evaluator.evaluate(policy(24, 48, 24, new BigDecimal("0.10")),
                new BigDecimal("100.00"), REQUESTED_AT);
        assertTrue(decision.allowed());
        assertEquals(new BigDecimal("90.00"), decision.refundAmount());
    }

    @Test
    void shouldRejectAfterDeadlineOrEventStart() {
        assertFalse(evaluator.evaluate(policy(23, null, null, null),
                new BigDecimal("100.00"), REQUESTED_AT).allowed());
        assertFalse(evaluator.evaluate(policy(0, null, null, null),
                new BigDecimal("100.00"), REQUESTED_AT).allowed());
        assertFalse(evaluator.evaluate(policy(-1, null, null, null),
                new BigDecimal("100.00"), REQUESTED_AT).allowed());
    }

    @Test
    void shouldRejectWhenRefundDisabled() {
        RefundPolicySnapshot snapshot = policy(72, null, null, null);
        snapshot.setAllowRefund(0);
        assertFalse(evaluator.evaluate(snapshot, new BigDecimal("100.00"), REQUESTED_AT).allowed());
    }

    private RefundPolicySnapshot policy(long hoursBeforeStart, Integer tier1, Integer tier2,
                                        BigDecimal feeRate) {
        RefundPolicySnapshot snapshot = new RefundPolicySnapshot();
        snapshot.setEventStartTime(Date.from(REQUESTED_AT.plusSeconds(hoursBeforeStart * 3600)));
        snapshot.setAllowRefund(1);
        snapshot.setTier1DeadlineHours(tier1);
        snapshot.setTier2DeadlineHours(tier2);
        snapshot.setTier2RefundFeeRate(feeRate);
        return snapshot;
    }
}
