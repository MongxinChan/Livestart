package com.mongxin.livestart.merchant.admin.service.basics.chain;

import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家后管责任链上下文容器
 * <p>
 * ApplicationContextAware 获取应用上下文；CommandLineRunner 项目启动后自动装配所有责任链 Handler。
 */
@Component
public final class MerchantAdminChainContext<T> implements ApplicationContextAware, CommandLineRunner {

    private ApplicationContext applicationContext;

    /**
     * 责任链容器
     * Key：责任链标识 mark()
     * Val：同一标识下按优先级排序的 Handler 集合
     */
    private final Map<String, List<MerchantAdminAbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * 执行责任链
     *
     * @param mark         责任链组件标识
     * @param requestParam 请求参数
     */
    public void handler(String mark, T requestParam) {
        List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtils.isEmpty(abstractChainHandlers)) {
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        abstractChainHandlers.forEach(each -> each.handler(requestParam));
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, MerchantAdminAbstractChainHandler> chainFilterMap = applicationContext.getBeansOfType(MerchantAdminAbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.getOrDefault(bean.mark(), new ArrayList<>());
            abstractChainHandlers.add(bean);
            abstractChainHandlerContainer.put(bean.mark(), abstractChainHandlers);
        });
        abstractChainHandlerContainer.forEach((mark, unsortedChainHandlers) -> {
            unsortedChainHandlers.sort(Comparator.comparing(Ordered::getOrder));
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
