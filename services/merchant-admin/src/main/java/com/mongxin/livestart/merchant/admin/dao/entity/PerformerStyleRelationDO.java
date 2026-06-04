package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 艺人与风格关联持久层实体
 * 对应表：t_performer_style_relation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_performer_style_relation")
public class PerformerStyleRelationDO {

    /**
     * 艺人ID
     */
    private Long performerId;

    /**
     * 风格ID
     */
    private Long styleId;
}
