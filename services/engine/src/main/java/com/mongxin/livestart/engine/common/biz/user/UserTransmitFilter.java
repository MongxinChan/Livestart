package com.mongxin.livestart.engine.common.biz.user;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户信息传递过滤器
 * <p>
 * 从网关注入的 Header 中解析用户信息，放入 ThreadLocal 上下文
 * Header Key 与网关的 TokenValidateFilter 保持一致
 */
@Component
public class UserTransmitFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String userId = request.getHeader("userId");
        String username = request.getHeader("username");
        String phone = request.getHeader("phone");
        String userType = request.getHeader("userType");

        // 有 userId 才设置（白名单接口无 userId Header）
        if (StrUtil.isNotBlank(userId)) {
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(userId)
                    .username(username)
                    .phone(phone)
                    .userType(StrUtil.isNotBlank(userType) ? Integer.valueOf(userType) : null)
                    .build();
            UserContext.setUser(userInfoDTO);
        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 请求结束后必须清理，防止内存泄漏
            UserContext.removeUser();
        }
    }
}
