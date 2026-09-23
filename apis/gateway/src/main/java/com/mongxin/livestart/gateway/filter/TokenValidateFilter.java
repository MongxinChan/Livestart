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
            "/api/live-start/admin/v1/user/login/code",
            "/api/live-start/admin/v1/user/send-code",
            "/api/live-start/pay/callback/**",
            "/api/pay/callback/**",
            "/api/live-start/engine/event/**",
            "/api/live-start/search/**"
    );
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String CLIENT_IP_HEADER = "X-Livestart-Client-IP";
    private static final String INTERNAL_TOKEN_HEADER = "X-Livestart-Internal-Token";
    private static final List<String> TRUSTED_HEADERS = List.of(
            "userId", "username", "phone", "realName", "userType",
            CLIENT_IP_HEADER, INTERNAL_TOKEN_HEADER);

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();
        ServerWebExchange sanitizedExchange = addClientIp(stripTrustedHeaders(exchange));
        if (isSkipPath(exchange)) {
            log.debug("[Gateway-Auth] Skip auth for path={}", requestPath);
            return chain.filter(sanitizedExchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst("token");
        if (StrUtil.isBlank(token)) {
            log.warn("[Gateway-Auth] Missing token header, path={}", requestPath);
            return writeUnauthorized(exchange, "请求缺少认证信息，请先登录");
        }

        String redisKey = USER_LOGIN_KEY + token;
        log.debug("[Gateway-Auth] Checking login state, path={}", requestPath);
        return reactiveStringRedisTemplate.opsForValue().get(redisKey)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[Gateway-Auth] Login record not found, path={}", requestPath);
                    return Mono.error(new RuntimeException("Login record not found"));
                }))
                .materialize()
                .flatMap(signal -> {
                    if (signal.isOnError()) {
                        Throwable error = signal.getThrowable();
                        if (error != null && "Login record not found".equals(error.getMessage())) {
                            return writeUnauthorized(exchange, "登录态已失效，请重新登录");
                        }
                        log.error("[Gateway-Auth] Login state lookup failed, path={}", requestPath, error);
                        return writeUnauthorized(exchange, "认证异常，请重试");
                    }
                    String userPayload = signal.get();
                    if (StrUtil.isBlank(userPayload) || "null".equals(userPayload)) {
                        log.warn("[Gateway-Auth] Invalid token, payload={}", userPayload);
                        return writeUnauthorized(exchange, "登录态已失效，请重新登录");
                    }

                    JSONObject userInfo = JSON.parseObject(userPayload);
                    Integer userType = userInfo.getInteger("userType");
                    if (!RolePermissionPolicy.isAllowed(requestPath, exchange.getRequest().getMethod(), userType)) {
                        log.warn("[Gateway-Auth] Forbidden, path={}, userId={}, userType={}",
                                requestPath, userInfo.getString("id"), userType);
                        return writeForbidden(exchange, "当前账号无权访问该后台功能");
                    }
                    ServerHttpRequest mutatedRequest = sanitizedExchange.getRequest().mutate()
                            .header("userId", valueOrEmpty(userInfo.getString("id")))
                            .header("username", valueOrEmpty(userInfo.getString("username")))
                            .header("phone", valueOrEmpty(userInfo.getString("phone")))
                            .header("realName", valueOrEmpty(userInfo.getString("realName")))
                            .header("userType", valueOrEmpty(userInfo.getString("userType")))
                            .build();

                    log.info("[Gateway-Auth] Auth success, userId={}, username={}", userInfo.getString("id"), userInfo.getString("username"));
                    return chain.filter(sanitizedExchange.mutate().request(mutatedRequest).build());
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean isSkipPath(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
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

    private ServerWebExchange addClientIp(ServerWebExchange exchange) {
        String clientIp = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CLIENT_IP_HEADER, clientIp)
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String msg) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, "A000004", msg);
    }

    private Mono<Void> writeForbidden(ServerWebExchange exchange, String msg) {
        return writeError(exchange, HttpStatus.FORBIDDEN, "A000005", msg);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "code", code,
                "message", msg,
                "data", ""
        );
        byte[] bytes = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
