package com.mongxin.livestart.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改常用观演人请求参数
 *
 * @author Mongxin
 */
@Data
public class VisitorUpdateReqDTO {

    /**
     * 观演人 ID
     */
    @NotNull(message = "观演人ID不能为空")
    private Long id;

    /**
     * 观演人真实姓名（可修改）
     */
    private String realName;

    /**
     * 观演人手机号（可修改）
     */
    private String mobile;
}
