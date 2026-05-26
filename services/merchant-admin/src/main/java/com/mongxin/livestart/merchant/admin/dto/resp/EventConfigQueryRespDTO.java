package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 演出配置详情查询接口返回参数
 */
@Data
@Schema(description = "演出配置详情查询返回实体")
public class EventConfigQueryRespDTO {

    @Schema(description = "对应演出ID")
    private Long eventId;

    @Schema(description = "选座模式 0:系统自动配座(高并发) 1:手动选座(剧场)")
    private Integer selectionMode;

    @Schema(description = "是否强制实名制入场 0:否 1:是")
    private Integer isVerifyRequired;

    @Schema(description = "单人账户最大购票上限")
    private Integer maxTicketsPerUser;

    @Schema(description = "退票政策 0:不可退 1:全额退 2:阶梯退票")
    private Integer refundPolicyType;

    @Schema(description = "全额退票截止时间(开演前X小时)")
    private Integer tier1FreeRefundHours;

    @Schema(description = "部分退票截止时间(开演前Y小时)")
    private Integer tier2PartialRefundHours;

    @Schema(description = "部分退票手续费比例(0.20代表20%)")
    private BigDecimal tier2RefundFeeRate;

    @Schema(description = "是否允许转赠门票 0:否 1:是")
    private Integer isTransferable;

    @Schema(description = "是否开启候补购票功能 0:否 1:是")
    private Integer isWaitingAllowed;
}
