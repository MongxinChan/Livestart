package com.mongxin.livestart.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 门票票档库存实体 (存储于 ds_common.t_ticket_sku)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_sku")
public class TicketSkuDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 演出ID
     */
    private Long eventId;

    /**
     * 票档名称/描述
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
     * 剩余可用库存
     */
    private Integer remainingStock;

    /**
     * 单人限购限秒张数
     */
    private Integer limitNum;

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;
}
