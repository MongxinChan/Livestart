package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量赠票任务创建请求 DTO
 */
@Data
@Schema(description = "批量门票发票赠送任务创建参数")
public class TicketTaskCreateReqDTO {

    @Schema(description = "任务名称 (如: 周杰伦赞助企业团拜送票)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "赠票任务名称不能为空")
    private String taskName;

    @Schema(description = "要发送的门票票档 SkuID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发票门票票档 SkuID 不能为空")
    private Long ticketSkuId;

    @Schema(description = "推送目标歌迷用户 Excel 表格 Url/测试本地路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Excel 文件链接不能为空")
    private String fileUrl;
}
