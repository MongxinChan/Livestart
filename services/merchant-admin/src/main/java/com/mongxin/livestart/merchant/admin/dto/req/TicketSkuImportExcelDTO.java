package com.mongxin.livestart.merchant.admin.dto.req;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票种批量导入 Excel 数据传输对象
 */
@Data
public class TicketSkuImportExcelDTO {

    /**
     * 关联演出ID
     */
    @ExcelProperty("eventId")
    private Long eventId;

    /**
     * 票种名称: 如 680元档/VIP区/早鸟
     */
    @ExcelProperty("title")
    private String title;

    /**
     * 票面原价
     */
    @ExcelProperty("originalPrice")
    private BigDecimal originalPrice;

    /**
     * 实际售价
     */
    @ExcelProperty("sellingPrice")
    private BigDecimal sellingPrice;

    /**
     * 总库存
     */
    @ExcelProperty("totalStock")
    private Integer totalStock;

    /**
     * 一开释放库存
     */
    @ExcelProperty("stage1Stock")
    private Integer stage1Stock;

    /**
     * 二开释放库存
     */
    @ExcelProperty("stage2Stock")
    private Integer stage2Stock;

    /**
     * 单人单次限购数量
     */
    @ExcelProperty("limitNum")
    private Integer limitNum;
}
