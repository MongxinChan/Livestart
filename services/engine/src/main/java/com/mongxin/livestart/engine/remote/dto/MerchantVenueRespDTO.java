package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

/**
 * merchant-admin 场馆查询响应（Feign 接收用）
 */
@Data
public class MerchantVenueRespDTO {

    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer capacity;
}
