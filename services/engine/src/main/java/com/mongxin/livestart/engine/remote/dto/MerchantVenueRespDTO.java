package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

/**
 * merchant-admin 场馆查询响应（Feign 接收用）
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

    /**
     * 拥有用户的id
     */
    private Long ownerUserId;
}
