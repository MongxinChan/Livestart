package com.mongxin.livestart.search.dto.resp;

import lombok.Data;
import java.util.Date;

@Data
public class EventSearchRespDTO {

    private Long id;

    private String title;

    private Integer eventType;

    private Long venueId;

    private Date startTime;

    private String posterUrl;

    private Integer status;
}
