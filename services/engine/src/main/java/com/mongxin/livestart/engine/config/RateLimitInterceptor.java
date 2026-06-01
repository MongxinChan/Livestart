package com.mongxin.livestart.engine.config;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.common.annotation.RateLimit;
import com.mongxin.livestart.engine.common.biz.user.UserContext;
import com.mongxin.livestart.engine.common.constant.EngineRedisConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Redis 滑动窗口限流拦截器
 * <p>
 * 对标注了 {@link RateLimit} 注解的 Controller 方法进行限流校验。
 * 基于 Redis ZSet 实现分布式滑动窗口算法，按 userId + URI 维度限流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LUA_PATH = "lua/rate_limit.lua";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查方法上是否有 @RateLimit 注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 获取用户ID
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            // 未登录用户使用 IP 作为限流维度
            userId = "ip:" + getClientIp(request);
        }

        // 构建限流 Key
        String uri = request.getRequestURI();
        String rateLimitKey = String.format(EngineRedisConstant.RATE_LIMIT_KEY, userId, uri);

        // 加载 Lua 脚本（单例缓存）
        DefaultRedisScript<Long> luaScript = Singleton.get(LUA_PATH, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_PATH)));
            script.setResultType(Long.class);
            return script;
        });

        // 执行限流判定
        long now = System.currentTimeMillis();
        String member = now + ":" + UUID.randomUUID().toString().substring(0, 8);

        Long result = stringRedisTemplate.execute(
                luaScript,
                List.of(rateLimitKey),
                String.valueOf(now),
                String.valueOf(rateLimit.timeWindowMs()),
                String.valueOf(rateLimit.permits()),
                member
        );

        if (result != null && result == 1L) {
            log.warn("[限流] 用户 {} 请求 {} 触发限流，窗口 {}ms 内超过 {} 次",
                    userId, uri, rateLimit.timeWindowMs(), rateLimit.permits());
            // 返回 HTTP 429 Too Many Requests
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> body = new HashMap<>();
            body.put("code", "429");
            body.put("message", "请求过于频繁，请稍后再试");
            body.put("data", null);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(JSON.toJSONString(body));
                writer.flush();
            }
            return false;
        }

        return true;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
