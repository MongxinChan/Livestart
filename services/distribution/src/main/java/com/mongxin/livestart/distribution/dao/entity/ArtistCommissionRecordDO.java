package com.mongxin.livestart.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 艺人专属推广票房提成分割及劳务个税明细 DO (存储于 ds_common.t_artist_commission_record)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_artist_commission_record")
public class ArtistCommissionRecordDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 主演/推广艺人账号用户ID
     */
    private Long artistId;

    /**
     * 专属宣发码
     */
    private String artistPromoCode;

    /**
     * 关联歌迷订单流水号 (或者已秒杀抢到的电子票ID)
     */
    private String orderNo;

    /**
     * 票房购票金额 (计算基数)
     */
    private BigDecimal ticketAmount;

    /**
     * 艺人分成比例 (默认为 10% 即 0.10)
     */
    private BigDecimal commissionRate;

    /**
     * 分成票房提成总额 (税前)
     */
    private BigDecimal commissionAmount;

    /**
     * 劳务报酬简易代扣个税/平台服务费率 (默认 20% 即 0.20)
     */
    private BigDecimal taxRate;

    /**
     * 代扣劳务个税/服务费金额
     */
    private BigDecimal taxAmount;

    /**
     * 艺人税后实际所得提成 (实发提成)
     */
    private BigDecimal actualAmount;

    /**
     * 结算状态 0:待结算 1:已结算到账 2:已退票取消
     */
    private Integer status;

    /**
     * 结算入账时间
     */
    private Date settleTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标记 0:未删除 1:已删除
     */
    @TableLogic
    private Integer delFlag;
}
