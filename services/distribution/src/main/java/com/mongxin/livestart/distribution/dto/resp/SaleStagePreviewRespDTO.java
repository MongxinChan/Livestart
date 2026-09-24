package com.mongxin.livestart.distribution.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 演出下一待开售阶段的概要信息，供客户端预约开售提醒。
 */
@Data
public class SaleStagePreviewRespDTO {

    /**
     * 分销演出 ID，提交提醒预约时使用。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventId;

    /**
     * 待开售阶段 ID，提交提醒预约时使用。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 阶段名称。
     */
    private String stageName;

    /**
     * 阶段计划开售时间，按北京时间返回。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;
}
