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

import java.util.Date;

/**
 * 歌迷秒杀/领取拥有的演出电子门票实体 (按 user_id 分片存储于 ds_order.t_user_ticket_${0..15})
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_ticket")
public class UserTicketDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 歌迷用户ID (分片列)
     */
    private Long userId;

    /**
     * 票档SkuID
     */
    private Long ticketSkuId;

    /**
     * 演出ID
     */
    private Long eventId;

    /**
     * 使用状态 0:未使用 1:已使用/已核销 2:已退票作废
     */
    private Integer status;

    /**
     * 唯一电子门票核销码 (UUID 32位)
     */
    private String checkCode;

    /**
     * 分销推荐来源艺人专属宣发推广码
     */
    private String artistPromoCode;

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
