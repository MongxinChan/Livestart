package com.mongxin.livestart.distribution.feign.dto;

import lombok.Data;

/**
 * 商户场馆信息 Feign 响应 DTO
 */
@Data
public class MerchantVenueRespDTO {

    /**
     * 场馆ID
     */
    private Long id;

    /**
     * 场馆名称
     */
    private String name;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 容纳人数
     */
    private Integer capacity;
}
