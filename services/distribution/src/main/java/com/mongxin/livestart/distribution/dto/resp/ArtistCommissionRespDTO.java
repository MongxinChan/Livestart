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

    /**
     * 提成记录主键ID
     */
    @Schema(description = "提成记录主键ID")
    private Long id;

    /**
     * 主演/推广艺人ID
     */
    @Schema(description = "主演/推广艺人ID")
    private Long artistId;

    /**
     * 专属宣发码
     */
    @Schema(description = "专属宣发码")
    private String artistPromoCode;

    /**
     * 关联的订单流水号
     */
    @Schema(description = "关联的订单流水号")
    private String orderNo;

    /**
     * 票房购票金额 (计算基数)
     */
    @Schema(description = "票房购票金额 (计算基数)")
    private BigDecimal ticketAmount;

    /**
     * 提成比例
     */
    @Schema(description = "提成比例")
    private BigDecimal commissionRate;

    /**
     * 分成票房提成总额 (税前)
     */
    @Schema(description = "分成票房提成总额 (税前)")
    private BigDecimal commissionAmount;

    /**
     * 代扣税率 (默认20%个税)
     */
    @Schema(description = "代扣税率 (默认20%个税)")
    private BigDecimal taxRate;

    /**
     * 代扣个税/服务费金额
     */
    @Schema(description = "代扣个税/服务费金额")
    private BigDecimal taxAmount;

    /**
     * 艺人实际到手金额 (已代扣税费)
     */
    @Schema(description = "艺人实际到手金额 (已代扣税费)")
    private BigDecimal actualAmount;

    /**
     * 结算状态 0:待结算 1:已结算 2:已取消
     */
    @Schema(description = "结算状态 0:待结算 1:已结算 2:已取消")
    private Integer status;

    /**
     * 结算状态描述
     */
    @Schema(description = "结算状态描述")
    private String statusDesc;

    /**
     * 到账结算时间
     */
    @Schema(description = "到账结算时间")
    private Date settleTime;

    /**
     * 创建时间(购票下单时间)
     */
    @Schema(description = "创建时间(购票下单时间)")
    private Date createTime;
}
