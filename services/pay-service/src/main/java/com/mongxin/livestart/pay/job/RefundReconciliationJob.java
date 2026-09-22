package com.mongxin.livestart.pay.job;

import com.mongxin.livestart.pay.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期查询支付宝退款结果，修正网络超时造成的本地处理中状态。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundReconciliationJob {

    private final RefundService refundService;

    @Scheduled(fixedDelayString = "${livestart.pay.refund-reconciliation.fixed-delay-ms:60000}")
    public void reconcile() {
        try {
            refundService.reconcilePendingRefunds();
        } catch (Exception ex) {
            log.error("[退款对账] 本轮对账任务执行失败", ex);
        }
    }
}
