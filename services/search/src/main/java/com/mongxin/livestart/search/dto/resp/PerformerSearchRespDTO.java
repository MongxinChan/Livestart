package com.mongxin.livestart.search.dto.resp;

import lombok.Data;

@Data
public class PerformerSearchRespDTO {

    private Long id;

    private String name;

    private Long styleId;

    private String avatar;

    private String bio;

    private Integer status;
}
