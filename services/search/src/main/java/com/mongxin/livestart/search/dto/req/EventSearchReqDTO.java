package com.mongxin.livestart.search.dto.req;

import lombok.Data;

/**
 * 演出搜索请求 DTO
 * 支持关键词、演出类型、城市、价格区间多维度过滤
 */
@Data
public class EventSearchReqDTO {

    /**
     * 关键词搜索（title 模糊匹配）
     */
    private String keyword;

    /**
     * 演出类型过滤：0=Livehouse, 1=演唱会，null=不限
     */
    private Integer eventType;

    /**
     * 城市过滤（如 "北京/北京市"），null=不限
     */
    private String city;

    /**
     * 价格区间下限（元），null=不限
     */
    private Integer minPrice;

    /**
     * 价格区间上限（元），null=不限
     */
    private Integer maxPrice;

    /**
     * 分页页码
     */
    private Integer pageNum = 1;

    /**
     * 分页大小
     */
    private Integer pageSize = 10;
}
