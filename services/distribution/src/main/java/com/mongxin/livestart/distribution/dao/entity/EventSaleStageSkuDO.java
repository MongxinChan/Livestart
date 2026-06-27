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
 * 演出售票阶段票档库存释放关联实体，映射到演出开售阶段票档释放表 {@code t_event_sale_stage_sku}。
 * 用于定义某一开售阶段中，各个票档（SKU）预设释放的库存量及是否已释放标记。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_event_sale_stage_sku")
public class EventSaleStageSkuDO {

    /**
     * 阶段票档配置主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联开售阶段ID（对应 t_event_sale_stage.id）
     */
    private Long stageId;

    /**
     * 冗余演出ID，便于在特定演出下快速查询
     */
    private Long eventId;

    /**
     * 关联票档ID（对应 t_ticket_sku.id）
     */
    private Long ticketSkuId;

    /**
     * 该阶段释放的票档库存数量
     */
    private Integer releaseStock;

    /**
     * 库存是否已释放状态（0:否，1:是）
     */
    private Integer releasedFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 逻辑删除标识（0:未删除，1:已删除）
     */
    @TableLogic
    private Integer delFlag;
}
