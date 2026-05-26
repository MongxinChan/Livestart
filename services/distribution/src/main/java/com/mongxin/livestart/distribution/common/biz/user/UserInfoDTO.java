package com.mongxin.livestart.distribution.common.biz.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨微服务透传用户信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    /**
     * 当前登录用户ID
     */
    private String userId;

    /**
     * 当前登录用户名
     */
    private String username;

    /**
     * 当前登录手机号
     */
    private String phone;
}
