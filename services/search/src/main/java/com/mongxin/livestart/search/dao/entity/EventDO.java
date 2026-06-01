package com.mongxin.livestart.search.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_event")
public class EventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Integer eventType;

    private Long venueId;

    private Date startTime;

    private String posterUrl;

    private Integer status;
}
