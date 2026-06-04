package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演出与风格关联持久层实体
 * 对应表：t_event_style_relation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_event_style_relation")
public class EventStyleRelationDO {

    /**
     * 演出ID
     */
    private Long eventId;

    /**
     * 风格ID
     */
    private Long styleId;
}
