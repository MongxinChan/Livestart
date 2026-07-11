package com.mongxin.livestart.merchant.admin.dto.req;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

/**
 * 演出活动批量导入 Excel 数据传输对象
 */
@Data
public class EventImportExcelDTO {

    /**
     * 演出标题
     */
    @ExcelProperty("title")
    private String title;

    /**
     * 演出类型 0:Livehouse(站票) 1:演唱会(选座)
     */
    @ExcelProperty("eventType")
    private Integer eventType;

    /**
     * 关联场馆ID
     */
    @ExcelProperty("venueId")
    private Long venueId;

    /**
     * 关联艺人ID
     */
    @ExcelProperty("performerId")
    private Long performerId;

    /**
     * 演出开始时间
     */
    @ExcelProperty("startTime")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 海报图片地址
     */
    @ExcelProperty("posterUrl")
    private String posterUrl;

    /**
     * 售票阶段 1: 一阶段售票, 2: 二阶段售票
     */
    @ExcelProperty("ticketStage")
    private Integer ticketStage;
}
