package com.mongxin.livestart.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增常用观演人请求参数
 *
 * @author Mongxin
 */
@Data
public class VisitorAddReqDTO {

    /**
     * 观演人真实姓名
     */
    @NotBlank(message = "观演人姓名不能为空")
    private String realName;

    /**
     * 证件类型 1:身份证 2:护照 3:港澳通行证 4:台胞证
     */
    @NotNull(message = "证件类型不能为空")
    private Integer cardType;

    /**
     * 证件号码（明文，后端负责校验+加密入库）
     */
    @NotBlank(message = "证件号不能为空")
    private String cardNo;

    /**
     * 观演人手机号（选填）
     */
    private String mobile;
}
