package com.mongxin.livestart.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 全局请求日志过滤器
 * 记录：TraceId、请求 URI、Method、耗时
 *
 * @author Mongxin
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();

        // 生成全链路追踪 ID
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();
        MDC.put("traceId", traceId);

        LOG.info("[Gateway] 请求开始 | traceId={} | {} {}", traceId, method, request.getURI());

        if (method == HttpMethod.GET) {
            LOG.info("[Gateway] 请求参数: {}", request.getQueryParams());
        }

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            LOG.info("[Gateway] 请求结束 | traceId={} | 耗时={}ms", traceId, duration);
            MDC.clear();
        }));
    }

    /**
     * 最高优先级，先于鉴权过滤器执行，记录完整请求链路
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
