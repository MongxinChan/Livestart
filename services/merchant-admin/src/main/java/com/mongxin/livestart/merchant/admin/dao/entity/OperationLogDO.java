package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 统一操作审计日志持久层实体
 * 对应表：t_operation_log（ds_common 公共库）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_operation_log")
public class OperationLogDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型 (如 Event, TicketSku)
     */
    private String type;

    /**
     * 业务单号 (演出ID / 票种ID 等)
     */
    private String bizNo;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作日志描述
     */
    private String operationLog;

    /**
     * 原始数据 (JSON)
     */
    private String originalData;

    /**
     * 修改后数据 (JSON)
     */
    private String modifiedData;

    /**
     * 创建时间
     */
    private Date createTime;
}
