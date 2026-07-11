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

    /**
     * 门票ID
     */
    @Schema(description = "门票ID")
    private Long id;

    /**
     * 演出ID
     */
    @Schema(description = "演出ID")
    private Long eventId;

    /**
     * 演出标题
     */
    @Schema(description = "演出标题")
    private String eventTitle;

    /**
     * 演出时间
     */
    @Schema(description = "演出时间")
    private Date eventTime;

    /**
     * 演出地点
     */
    @Schema(description = "演出地点")
    private String address;

    /**
     * 票档SkuID
     */
    @Schema(description = "票档SkuID")
    private Long ticketSkuId;

    /**
     * 票档名称 (如: 内场1280)
     */
    @Schema(description = "票档名称 (如: 内场1280)")
    private String ticketSkuTitle;

    /**
     * 购票/领票金额
     */
    @Schema(description = "购票/领票金额")
    private BigDecimal price;

    /**
     * 门票状态 0:未使用 1:已核销 2:已退票
     */
    @Schema(description = "门票状态 0:未使用 1:已核销 2:已退票")
    private Integer status;

    /**
     * 门票状态描述
     */
    @Schema(description = "门票状态描述")
    private String statusDesc;

    /**
     * 32位电子门票唯一核销码
     */
    @Schema(description = "32位电子门票唯一核销码")
    private String checkCode;

    /**
     * 来源推广码
     */
    @Schema(description = "来源推广码")
    private String artistPromoCode;

    /**
     * 获得/购票时间
     */
    @Schema(description = "获得/购票时间")
    private Date createTime;
}
