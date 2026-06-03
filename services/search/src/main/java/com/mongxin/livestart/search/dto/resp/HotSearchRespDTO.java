package com.mongxin.livestart.search.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热搜关键词响应 DTO（含搜索热度分值）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热搜关键词")
public class HotSearchRespDTO {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "热度分值（Redis ZSet score）")
    private Double score;
}
