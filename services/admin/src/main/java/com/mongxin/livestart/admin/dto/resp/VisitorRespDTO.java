package com.mongxin.livestart.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongxin.livestart.admin.common.serialize.IdCardDesensitizationSerializer;
import lombok.Data;

/**
 * 观演人信息响应 DTO
 *
 * @author Mongxin
 */
@Data
public class VisitorRespDTO {

    /**
     * 观演人 ID
     */
    private Long id;

    /**
     * 所属用户 ID
     */
    private Long userId;

    /**
     * 观演人真实姓名
     */
    private String realName;

    /**
     * 证件类型 1:身份证 2:护照 3:港澳通行证 4:台胞证
     */
    private Integer cardType;

    /**
     * 证件类型描述（中文）
     */
    private String cardTypeDesc;

    /**
     * 证件号码（脱敏输出）
     */
    @JsonSerialize(using = IdCardDesensitizationSerializer.class)
    private String cardNo;

    /**
     * 观演人手机号
     */
    private String mobile;
}
