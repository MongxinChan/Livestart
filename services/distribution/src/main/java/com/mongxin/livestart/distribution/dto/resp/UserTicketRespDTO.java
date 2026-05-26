package com.mongxin.livestart.distribution.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 歌迷持有的门票响应数据实体
 */
@Data
@Schema(description = "歌迷持有电子门票信息")
public class UserTicketRespDTO {

    @Schema(description = "门票ID")
    private Long id;

    @Schema(description = "演出ID")
    private Long eventId;

    @Schema(description = "演出标题")
    private String eventTitle;

    @Schema(description = "演出时间")
    private Date eventTime;

    @Schema(description = "演出地点")
    private String address;

    @Schema(description = "票档SkuID")
    private Long ticketSkuId;

    @Schema(description = "票档名称 (如: 内场1280)")
    private String ticketSkuTitle;

    @Schema(description = "购票/领票金额")
    private BigDecimal price;

    @Schema(description = "门票状态 0:未使用 1:已核销 2:已退票")
    private Integer status;

    @Schema(description = "门票状态描述")
    private String statusDesc;

    @Schema(description = "32位电子门票唯一核销码")
    private String checkCode;

    @Schema(description = "来源推广码")
    private String artistPromoCode;

    @Schema(description = "获得/购票时间")
    private Date createTime;
}
