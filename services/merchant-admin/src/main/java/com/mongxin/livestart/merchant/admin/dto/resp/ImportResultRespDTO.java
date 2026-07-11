package com.mongxin.livestart.merchant.admin.dto.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果响应 DTO
 */
@Data
public class ImportResultRespDTO {

    /**
     * 总导入记录数
     */
    private Integer total = 0;

    /**
     * 导入成功记录数
     */
    private Integer success = 0;

    /**
     * 导入失败记录数
     */
    private Integer fail = 0;

    /**
     * 导入错误明细列表
     */
    private List<ImportErrorRespDTO> errors = new ArrayList<>();

    public void addSuccess() {
        total++;
        success++;
    }

    public void addFail(Integer rowIndex, String message) {
        total++;
        fail++;
        errors.add(new ImportErrorRespDTO(rowIndex, message));
    }
}
