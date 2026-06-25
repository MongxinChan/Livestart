package com.mongxin.livestart.settlement.common.biz.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算服务用户上下文信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    private String userId;
    private String username;
    private String phone;
    private String realName;
    private Integer userType;
}
