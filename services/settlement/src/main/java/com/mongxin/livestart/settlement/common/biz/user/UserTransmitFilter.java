package com.mongxin.livestart.settlement.common.biz.user;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;

/**
 * 从请求头透传用户信息到结算服务上下文
 */
public class UserTransmitFilter implements Filter {

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String userId = request.getHeader("userId");
        if (StrUtil.isNotBlank(userId)) {
            String username = request.getHeader("username");
            String realName = request.getHeader("realName");
            String phone = request.getHeader("phone");
            String userType = request.getHeader("userType");
            UserContext.setUser(new UserInfoDTO(
                    userId,
                    username,
                    phone,
                    realName,
                    StrUtil.isNotBlank(userType) ? Integer.valueOf(userType) : null
            ));
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}
