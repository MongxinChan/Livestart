package com.mongxin.livestart.distribution.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 分销端演出开售阶段响应 DTO
 */
@Data
public class EventSaleStageRespDTO {

    /**
     * 阶段主键ID
     */
    private Long id;

    /**
     * 关联演出ID
     */
    private Long eventId;

    /**
     * 阶段序号（1:一开，2:二开...）
     */
    private Integer stageNo;

    /**
     * 阶段名称
     */
    private String stageName;

    /**
     * 该阶段统一开售时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 阶段状态 0:待开售 1:已开售 2:已完成 3:已取消
     */
    private Integer status;
}
