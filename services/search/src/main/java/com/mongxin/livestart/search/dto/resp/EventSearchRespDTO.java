package com.mongxin.livestart.search.dto.resp;

import lombok.Data;
import java.util.Date;

/**
 * 演出搜索结果 DTO
 * <p>
 * 原始数据库字段（保留兼容）+ 前端 LiveEvent 对齐字段（由 Service 层填充）
 */
@Data
public class EventSearchRespDTO {

    // ---- 原始字段 ----
    private Long id;
    private String title;
    private Integer eventType;
    private Long venueId;
    private Date startTime;
    private String posterUrl;
    private Integer status;

    // ---- 前端 LiveEvent 对齐字段（Service 层填充）----
    /** 演出类型文本，如 "演唱会" / "Livehouse" */
    private String type;
    /** 封面图 URL（来源于 posterUrl）*/
    private String cover;
    /** 格式化后的演出时间，如 "2026-09-01 20:00" */
    private String date;
    /** 场馆名称（暂为兜底文案，待关联 venue 表）*/
    private String venue;
    /** 艺人（暂留空，待关联 performer 表）*/
    private String artist;
    /** 最低价格（暂留 0，待关联 sku 表）*/
    private Integer minPrice;
}
