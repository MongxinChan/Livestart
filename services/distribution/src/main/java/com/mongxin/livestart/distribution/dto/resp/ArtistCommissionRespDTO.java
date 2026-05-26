package com.mongxin.livestart.distribution.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 艺人推广统计与税后分成收益响应数据实体
 */
@Data
@Schema(description = "艺人推广个税代扣及提成统计响应")
public class ArtistCommissionRespDTO {

    @Schema(description = "提成记录主键ID")
    private Long id;

    @Schema(description = "主演/推广艺人ID")
    private Long artistId;

    @Schema(description = "专属宣发码")
    private String artistPromoCode;

    @Schema(description = "关联的订单流水号")
    private String orderNo;

    @Schema(description = "票房购票金额 (计算基数)")
    private BigDecimal ticketAmount;

    @Schema(description = "提成比例")
    private BigDecimal commissionRate;

    @Schema(description = "分成票房提成总额 (税前)")
    private BigDecimal commissionAmount;

    @Schema(description = "代扣税率 (默认20%个税)")
    private BigDecimal taxRate;

    @Schema(description = "代扣个税/服务费金额")
    private BigDecimal taxAmount;

    @Schema(description = "艺人实际到手金额 (已代扣税费)")
    private BigDecimal actualAmount;

    @Schema(description = "结算状态 0:待结算 1:已结算 2:已取消")
    private Integer status;

    @Schema(description = "结算状态描述")
    private String statusDesc;

    @Schema(description = "到账结算时间")
    private Date settleTime;

    @Schema(description = "创建时间(购票下单时间)")
    private Date createTime;
}
