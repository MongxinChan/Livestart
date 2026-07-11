package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

/**
 * 管理端用户简要信息响应 DTO
 */
@Data
public class AdminUserSimpleRespDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名 (昵称)
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;
}
