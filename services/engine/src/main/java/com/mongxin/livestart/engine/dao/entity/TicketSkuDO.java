package com.mongxin.livestart.engine.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票种 DO（只读，对应公共库 live_start.t_ticket_sku）
 */
@Data
@TableName("t_ticket_sku")
public class TicketSkuDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演出ID
     */
    private Long eventId;

    /**
     * 票种名称
     */
    private String title;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 售价
     */
    private BigDecimal sellingPrice;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 剩余库存
     */
    private Integer remainingStock;

    /**
     * 单人购票上限
     */
    private Integer limitNum;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;
}
