package com.mongxin.livestart.merchant.admin.toolkit;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.mongxin.livestart.framework.exception.ClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class EasyExcelImportUtil {

    private EasyExcelImportUtil() {
    }

    public static <T> List<T> readFirstSheet(MultipartFile file, Class<T> clazz) {
        validateFile(file);
        try {
            return EasyExcel.read(file.getInputStream())
                    .head(clazz)
                    .sheet()
                    .doReadSync();
        } catch (ExcelAnalysisException ex) {
            throw new ClientException("Excel 解析失败，请检查表头、单元格格式与模板是否一致：" + friendlyMessage(ex));
        } catch (IOException ex) {
            throw new ClientException("Excel 文件读取失败，请重新上传");
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("Excel 文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return;
        }
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".xls")) {
            throw new ClientException("请上传 .xlsx 或 .xls 格式的 Excel 文件");
        }
    }

    private static String friendlyMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "文件内容不符合 Excel 导入模板";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
