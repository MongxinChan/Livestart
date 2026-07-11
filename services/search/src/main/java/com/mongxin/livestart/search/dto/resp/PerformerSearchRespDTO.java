package com.mongxin.livestart.search.dto.resp;

import lombok.Data;

/**
 * 艺人搜索响应 DTO
 */
@Data
public class PerformerSearchRespDTO {

    /**
     * 艺人ID
     */
    private Long id;

    /**
     * 艺人/乐队名称
     */
    private String name;

    /**
     * 音乐风格ID
     */
    private Long styleId;

    /**
     * 艺人头像
     */
    private String avatar;

    /**
     * 介绍
     */
    private String bio;

    /**
     * 状态 1:正常 0:停演
     */
    private Integer status;
}
