package com.mongxin.livestart.search.dto.resp;

import lombok.Data;
import java.util.Date;
import java.math.BigDecimal;

/**
 * 演出搜索结果 DTO
 * <p>
 * 原始数据库字段（保留兼容）+ 前端 LiveEvent 对齐字段（由 Service 层填充）
 */
@Data
public class EventSearchRespDTO {

    // ---- 原始字段 ----
    /**
     * 演出活动ID
     */
    private Long id;

    /**
     * 演出标题
     */
    private String title;

    /**
     * 演出类型 0:Livehouse(站票) 1:演唱会(选座)
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
     * 海报封面图片 URL
     */
    private String posterUrl;

    /**
     * 演出状态 0:下架 1:预售 2:上架 3:售罄
     */
    private Integer status;

    // ---- 前端 LiveEvent 对齐字段（Service 层填充）----
    /**
     * 演出类型文本，如 "演唱会" / "Livehouse"
     */
    private String type;

    /**
     * 封面图 URL（来源于 posterUrl）
     */
    private String cover;

    /**
     * 格式化后的演出时间，如 "2026-09-01 20:00"
     */
    private String date;

    /**
     * 场馆名称
     */
    private String venue;

    /** 场馆所在城市 */
    private String city;

    /**
     * 艺人
     */
    private String artist;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;
}
