package com.mongxin.livestart.distribution.service.basics;

/**
 * 门票批量推送及分销策略执行接口
 */
public interface DistributionExecuteStrategy<REQUEST, RESPONSE> {

    /**
     * 策略模式的唯一标识
     */
    String mark();

    /**
     * 无返回值的执行
     *
     * @param requestParam 执行入参
     */
    default void execute(REQUEST requestParam) {
    }

    /**
     * 带返回值的执行
     *
     * @param requestParam 执行入参
     * @return 执行返回值
     */
    default RESPONSE executeResp(REQUEST requestParam) {
        return null;
    }
}
