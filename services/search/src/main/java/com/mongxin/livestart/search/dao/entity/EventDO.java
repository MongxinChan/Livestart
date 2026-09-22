package com.mongxin.livestart.search.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.math.BigDecimal;

/**
 * 演出活动搜索引擎文档实体，对应表：t_event
 */
@Data
@TableName("t_event")
public class EventDO {

    /**
     * 演出活动ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演出标题
     */
    private String title;

    /**
     * 演出类型/音乐风格
     */
    private Integer eventType;

    /**
     * 演出场馆ID
     */
    private Long venueId;

    /**
     * 演出开始时间
     */
    private Date startTime;

    /**
     * 海报封面 URL
     */
    private String posterUrl;

    /**
     * 演出状态 0:下架 1:预售 2:上架 3:售罄
     */
    private Integer status;

    /** 关联场馆名称和城市，由搜索查询别名映射。 */
    private String venueName;
    private String venueCity;

    /** 关联艺人名称，由搜索查询聚合。 */
    private String performerName;

    /** 票档最低售价，由搜索查询聚合。 */
    private BigDecimal minPrice;
}
