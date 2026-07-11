package com.mongxin.livestart.engine.remote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * merchant-admin 演出查询响应（Feign 接收用）
 */
@Data
public class MerchantEventRespDTO {

    /**
     * 演出活动ID
     */
    private Long id;

    /**
     * 演出标题
     */
    private String title;

    /**
     * 演出类型/风格ID
     */
    private Integer eventType;

    /**
     * 演出场馆ID
     */
    private Long venueId;

    /**
     * 演出开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 演出海报/封面图片URL
     */
    private String posterUrl;

    /**
     * 演出状态 0:下架 1:预售 2:上架(售票中) 3:售罄
     */
    private Integer status;

    /**
     * 参演艺人ID
     */
    private Long performerId;

    /**
     * 参演艺人/团体名称
     */
    private String performerName;

    /**
     * 售票阶段 (1: 一阶段售票, 2: 二阶段售票)
     */
    private Integer ticketStage;
}

