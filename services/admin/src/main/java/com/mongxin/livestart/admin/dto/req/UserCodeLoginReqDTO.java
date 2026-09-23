package com.mongxin.livestart.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 手机验证码登录请求参数。
 */
@Data
public class UserCodeLoginReqDTO {

    /**
     * 接收登录验证码的手机号。
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号")
    private String phone;

    /**
     * 用户收到的六位短信验证码。
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "请输入 6 位数字验证码")
    private String code;
}
