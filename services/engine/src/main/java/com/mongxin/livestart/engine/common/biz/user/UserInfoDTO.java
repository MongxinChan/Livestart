package com.mongxin.livestart.engine.common.biz.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    /** 用户 ID */
    private String userId;

    /** 用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 用户类型：1-乐迷 2-艺人 3-场地管理员 4-超管 */
    private Integer userType;
}
