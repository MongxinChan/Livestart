package com.mongxin.livestart.distribution.feign.dto;

import lombok.Data;

@Data
public class MerchantVenueRespDTO {

    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer capacity;
}
