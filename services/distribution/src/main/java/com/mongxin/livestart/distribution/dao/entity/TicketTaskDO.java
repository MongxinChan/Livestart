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
 * 门票批量推送赠送任务实体 (存储于 ds_common.t_ticket_task)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_task")
public class TicketTaskDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 门票SkuID
     */
    private Long ticketSkuId;

    /**
     * 名单Excel文件Url/测试本地路径
     */
    private String fileUrl;

    /**
     * 任务执行状态 0:待执行 1:执行中 2:已完成 3:失败
     */
    private Integer status;

    /**
     * 推送总歌迷数
     */
    private Integer totalCount;

    /**
     * 成功赠票出票数
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 操作人
     */
    private String operator;

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
