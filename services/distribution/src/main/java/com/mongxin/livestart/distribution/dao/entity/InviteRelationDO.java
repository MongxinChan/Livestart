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
 * 歌迷分销推广关系绑定实体 (存储于 ds_common.t_invite_relation)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_invite_relation")
public class InviteRelationDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 推广人/艺人用户ID
     */
    private Long inviterUserId;

    /**
     * 被推广人/歌迷用户ID
     */
    private Long inviteeUserId;

    /**
     * 绑定的专属推广宣发码
     */
    private String inviteCode;

    /**
     * 绑定时间
     */
    private Date bindTime;

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
