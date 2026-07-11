package com.mongxin.livestart.merchant.admin.dto.req;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 艺人批量导入 Excel 数据传输对象
 */
@Data
public class PerformerImportExcelDTO {

    /**
     * 艺人/乐队名称
     */
    @ExcelProperty("name")
    private String name;

    /**
     * 关联风格ID
     */
    @ExcelProperty("styleId")
    private Long styleId;

    /**
     * 艺人头像
     */
    @ExcelProperty("avatar")
    private String avatar;

    /**
     * 介绍
     */
    @ExcelProperty("bio")
    private String bio;

    /**
     * 状态 1:正常 0:停演
     */
    @ExcelProperty("status")
    private Integer status;
}
