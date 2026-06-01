package com.mongxin.livestart.engine.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单主表（分16表：t_order_{0..15}，按 user_id 分片）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order")
public class OrderDO {

    /**
     * 订单ID（雪花算法生成）
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 订单流水号（展示给用户，可读格式）
     */
    private String orderNo;

    /**
     * 下单用户ID（Sharding Key）
     */
    private Long userId;

    /**
     * 实付总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态 0:待支付 1:已支付 2:已取消 3:已退票
     */
    private Integer status;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 创建时间
     */
    private Date createTime;
}
