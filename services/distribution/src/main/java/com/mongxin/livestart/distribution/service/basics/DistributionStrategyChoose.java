package com.mongxin.livestart.distribution.service.basics;

import com.mongxin.livestart.framework.exception.ServiceException;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 分发模块策略选择路由组件
 */
@Component
@SuppressWarnings("rawtypes")
public class DistributionStrategyChoose implements ApplicationContextAware, CommandLineRunner {

    private ApplicationContext applicationContext;
    private final Map<String, DistributionExecuteStrategy> abstractExecuteStrategyMap = new HashMap<>();

    /**
     * 根据标识路由到具体策略
     *
     * @param mark 策略标识
     * @return 实际策略实例
     */
    public DistributionExecuteStrategy choose(String mark) {
        return Optional.ofNullable(abstractExecuteStrategyMap.get(mark))
                .orElseThrow(() -> new ServiceException(String.format("分发策略 [%s] 未定义", mark)));
    }

    /**
     * 选择并执行对应策略 (无返回值)
     */
    @SuppressWarnings("unchecked")
    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam) {
        DistributionExecuteStrategy executeStrategy = choose(mark);
        executeStrategy.execute(requestParam);
    }

    /**
     * 选择并执行对应策略 (带返回值)
     */
    @SuppressWarnings("unchecked")
    public <REQUEST, RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam) {
        DistributionExecuteStrategy executeStrategy = choose(mark);
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, DistributionExecuteStrategy> actual = applicationContext.getBeansOfType(DistributionExecuteStrategy.class);
        actual.forEach((beanName, bean) -> {
            DistributionExecuteStrategy beanExist = abstractExecuteStrategyMap.get(bean.mark());
            if (beanExist != null) {
                throw new ServiceException(String.format("策略 [%s] 重复定义", bean.mark()));
            }
            abstractExecuteStrategyMap.put(bean.mark(), bean);
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
