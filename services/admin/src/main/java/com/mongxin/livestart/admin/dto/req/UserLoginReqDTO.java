package com.mongxin.livestart.admin.dto.req;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求参数
 */
@Data
public class UserLoginReqDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "登录手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "登录密码不能为空")
    private String password;
}
