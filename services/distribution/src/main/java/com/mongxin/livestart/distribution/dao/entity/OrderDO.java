package com.mongxin.livestart.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 简易订单 DO (用于 distribution 微服务对订单分表的只读路由安全查询)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order")
public class OrderDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String orderNo;

    /**
     * 下单人ID (Sharding Column)
     */
    private Long userId;

    /**
     * 实际支付的票款金额
     */
    private BigDecimal totalAmount;

    private Integer status;
}
