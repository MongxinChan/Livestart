package com.mongxin.livestart.admin.dto.req;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户注册请求参数
 *
 * @author Mongxin
 */
@Data
public class UserRegisterReqDTO {

    /**
     * 用户名 (非必填，若为空则由系统随机分配默认名)
     */
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 身份证号
     */
    private String idCard;
}
