package com.mongxin.livestart.merchant.admin.dto.req;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 场馆批量导入 Excel 数据传输对象
 */
@Data
public class VenueImportExcelDTO {

    /**
     * 场馆名称
     */
    @ExcelProperty("name")
    private String name;

    /**
     * 所在城市
     */
    @ExcelProperty("city")
    private String city;

    /**
     * 详细地址
     */
    @ExcelProperty("address")
    private String address;

    /**
     * 场馆总容纳人数
     */
    @ExcelProperty("capacity")
    private Integer capacity;

    /**
     * 归属场地管理员用户ID
     */
    @ExcelProperty("ownerUserId")
    private Long ownerUserId;
}
