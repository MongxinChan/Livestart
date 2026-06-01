package com.mongxin.livestart.engine.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单明细 / 电子票（分16表：t_order_item_{0..15}，与 t_order Binding Table）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order_item")
public class OrderItemDO {

    /**
     * 电子票ID（雪花算法生成）
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 所属订单流水号
     */
    private String orderNo;

    /**
     * 下单用户ID（Sharding Key，与 t_order 保持一致）
     */
    private Long userId;

    /**
     * 实际观演人ID（来自 t_user_visitor）
     */
    private Long visitorId;

    /**
     * 演出ID
     */
    private Long eventId;

    /**
     * 票种ID
     */
    private Long skuId;

    /**
     * 座位ID（选座模式下才有）
     */
    private Long seatId;

    /**
     * 电子票唯一核销码（入场时扫描验证）
     */
    private String checkCode;

    /**
     * 入场状态 0:未入场 1:已入场
     */
    private Integer isChecked;
}
