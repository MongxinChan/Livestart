package com.mongxin.livestart.engine.remote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * merchant-admin 演出查询响应（Feign 接收用）
 */
@Data
public class MerchantEventRespDTO {

    private Long id;
    private String title;
    private Integer eventType;
    private Long venueId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    private String posterUrl;
    private Integer status;
}
