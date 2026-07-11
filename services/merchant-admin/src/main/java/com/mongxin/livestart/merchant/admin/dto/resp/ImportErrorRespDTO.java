package com.mongxin.livestart.merchant.admin.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入错误详情响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorRespDTO {

    /**
     * Excel 导入解析出错的行号，从 0 开始
     */
    private Integer rowIndex;

    /**
     * 错误提示信息
     */
    private String message;
}
