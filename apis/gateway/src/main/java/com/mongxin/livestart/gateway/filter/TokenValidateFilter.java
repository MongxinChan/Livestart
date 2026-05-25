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

/**
 * Token 鉴权全局过滤器
 *
 * <p>
 * 核心职责：
 * <ol>
 * <li>白名单路径直接放行（注册、登录等）</li>
 * <li>从请求 Header 中获取 {@code phone} 和 {@code token}，校验 Redis 登录态</li>
 * <li>校验通过后，将 userId / username / phone / realName 注入到下游 Header</li>
 * <li>校验失败，返回 HTTP 401 统一错误响应</li>
 * </ol>
 *
 * <p>
 * 与下游服务的配合：
 * 下游的 {@code UserTransmitFilter} 从 Header 读取用户信息写入 {@code UserContext}，
 * 两者串联即可打通用户上下文全链路。
 *
 * @author Mongxin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidateFilter implements GlobalFilter, Ordered {

    /**
     * Redis 登录态 Hash Key 前缀（与 admin 服务 RedisCacheConstant.USER_LOGIN_KEY 保持一致）
     */
    private static final String USER_LOGIN_KEY = "live-start:login:";

    /**
     * 白名单路径：这些接口无需登录即可访问
     */
    private static final List<String> SKIP_PATHS = List.of(
            "/api/live-start/admin/v1/user/login", // 用户登录
            "/api/live-start/admin/v1/user", // 用户注册 (POST)
            "/api/live-start/admin/v1/has-phone/**", // 手机号查重
            "/api/live-start/admin/v1/user/check-login", // 登录态查询（内部用）
            "/api/live-start/merchant-admin/**" // 商户后台 MVP 阶段暂不鉴权
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();

        // 1. 白名单放行
        if (isSkipPath(requestPath)) {
            log.debug("[Gateway-Auth] 白名单放行: {}", requestPath);
            return chain.filter(exchange);
        }

        // 2. 从 Header 获取鉴权参数
        String phone = exchange.getRequest().getHeaders().getFirst("phone");
        String token = exchange.getRequest().getHeaders().getFirst("token");

        if (StrUtil.isBlank(phone) || StrUtil.isBlank(token)) {
            log.warn("[Gateway-Auth] 缺少鉴权 Header | path={} | phone={}", requestPath, phone);
            return writeUnauthorized(exchange, "请求缺少认证信息，请先登录");
        }

        // 3. 查询 Redis 校验 token（响应式非阻塞）
        String redisKey = USER_LOGIN_KEY + phone;
        return reactiveStringRedisTemplate
                .opsForHash()
                .get(redisKey, token)
                .flatMap(userJson -> {
                    if (StrUtil.isBlank((String) userJson)) {
                        log.warn("[Gateway-Auth] Token 无效或已过期 | phone={}", phone);
                        return writeUnauthorized(exchange, "登录态已失效，请重新登录");
                    }

                    // 4. 解析用户信息，注入下游 Header
                    JSONObject userInfo = JSON.parseObject((String) userJson);
                    String userId = userInfo.getString("id");
                    String username = userInfo.getString("username");
                    String realName = userInfo.getString("realName");

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("userId", userId != null ? userId : "")
                            .header("username", username != null ? username : "")
                            .header("phone", phone)
                            .header("realName", realName != null ? realName : "")
                            .build();

                    log.debug("[Gateway-Auth] 鉴权通过 | userId={} | path={}", userId, requestPath);
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(
                        // Redis 中不存在该 key（未登录或已过期）
                        Mono.defer(() -> {
                            log.warn("[Gateway-Auth] Redis 中无登录记录 | phone={}", phone);
                            return writeUnauthorized(exchange, "登录态已失效，请重新登录");
                        }));
    }

    /**
     * 鉴权过滤器顺序：在日志过滤器（-1）之后，在业务过滤器之前
     */
    @Override
    public int getOrder() {
        return 0;
    }

    // ========================= 私有工具方法 =========================

    /**
     * 判断路径是否在白名单中
     */
    private boolean isSkipPath(String requestPath) {
        for (String skipPath : SKIP_PATHS) {
            if (PATH_MATCHER.match(skipPath, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造 HTTP 401 响应，写入统一 JSON 格式错误体
     */
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "code", "A000004",
                "message", message,
                "data", "");
        byte[] bytes = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
