package com.mongxin.livestart.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidateFilter implements GlobalFilter, Ordered {

    private static final String USER_LOGIN_KEY = "live-start:login:";
    private static final List<String> SKIP_PATHS = List.of(
            "/api/live-start/admin/v1/user/login",
            "/api/live-start/admin/v1/user/login/code",
            "/api/live-start/admin/v1/user/send-code",
            "/api/live-start/admin/v1/user",
            "/api/live-start/admin/v1/has-phone/**",
            "/api/live-start/admin/v1/user/check-login",
            "/api/live-start/engine/order/pay/alipay/notify",
            "/api/engine/order/pay/alipay/notify",
            "/api/live-start/engine/event/**",
            "/api/live-start/search/**"
    );
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> TRUSTED_HEADERS = List.of("userId", "username", "realName");

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();
        if (isSkipPath(requestPath)) {
            log.debug("[Gateway-Auth] Skip auth for path={}", requestPath);
            return chain.filter(stripTrustedHeaders(exchange));
        }

        String phone = exchange.getRequest().getHeaders().getFirst("phone");
        String token = exchange.getRequest().getHeaders().getFirst("token");
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(token)) {
            log.warn("[Gateway-Auth] Missing auth header, path={}, phone={}", requestPath, phone);
            return writeUnauthorized(exchange, "请求缺少认证信息，请先登录");
        }

        String redisKey = USER_LOGIN_KEY + phone;
        return reactiveStringRedisTemplate.opsForHash().get(redisKey, token)
                .flatMap(userJson -> {
                    String userPayload = (String) userJson;
                    if (StrUtil.isBlank(userPayload)) {
                        log.warn("[Gateway-Auth] Invalid token, phone={}", phone);
                        return writeUnauthorized(exchange, "登录态已失效，请重新登录");
                    }

                    JSONObject userInfo = JSON.parseObject(userPayload);
                    ServerHttpRequest mutatedRequest = stripTrustedHeaders(exchange).getRequest().mutate()
                            .header("userId", valueOrEmpty(userInfo.getString("id")))
                            .header("username", valueOrEmpty(userInfo.getString("username")))
                            .header("phone", phone)
                            .header("realName", valueOrEmpty(userInfo.getString("realName")))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[Gateway-Auth] Login record not found in redis, phone={}", phone);
                    return writeUnauthorized(exchange, "登录态已失效，请重新登录");
                }));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean isSkipPath(String requestPath) {
        for (String skipPath : SKIP_PATHS) {
            if (PATH_MATCHER.match(skipPath, requestPath)) {
                return true;
            }
        }
        return false;
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private ServerWebExchange stripTrustedHeaders(ServerWebExchange exchange) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        for (String header : TRUSTED_HEADERS) {
            builder.headers(h -> h.remove(header));
        }
        return exchange.mutate().request(builder.build()).build();
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "code", "A000004",
                "message", msg,
                "data", ""
        );
        byte[] bytes = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
