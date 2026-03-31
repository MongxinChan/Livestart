package com.mongxin.livestart.merchant.admin.dao.entity;

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

    /**
     * 演出标题
     */
    private String title;

    /**
     * 演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)
     */
    private Integer eventType;

    /**
     * 关联场馆ID
     */
    private Long venueId;

    /**
     * 演出开始时间
     */
    private Date startTime;

    /**
     * 海报图片地址
     */
    private String posterUrl;

    /**
     * 状态 0:下架 1:预售 2:在售 3:售罄
     */
    private Integer status;
}
