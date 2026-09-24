package com.mongxin.livestart.distribution.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 分销演出的开售阶段响应 DTO。
 */
@Data
public class SaleStageRespDTO {

    /** 阶段主键 ID。 */
    private Long id;

    /** 分销演出 ID。 */
    private Long eventId;

    /** 阶段序号。 */
    private Integer stageNo;

    /** 阶段名称。 */
    private String stageName;

    /** 阶段开售时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /** 阶段状态：0 待开售，1 已开售，2 已完成，3 已取消。 */
    private Integer status;
}
