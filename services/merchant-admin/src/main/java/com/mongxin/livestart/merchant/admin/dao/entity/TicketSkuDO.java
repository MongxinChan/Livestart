package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("ticket_skus")
public class TicketSkuDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联演出ID
     */
    private Long eventId;

    /**
     * 票种名称: 如 680元档/VIP区/早鸟
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
     * 总库存 (初始录入)
     */
    private Integer totalStock;

    /**
     * 当前剩余库存 (扣减锚点)
     */
    private Integer remainingStock;

    /**
     * 单人限购数量
     */
    private Integer limitNum;

    /**
     * 乐观锁版本号(备用防超卖防线)
     */
    @Version
    private Integer version;
}
