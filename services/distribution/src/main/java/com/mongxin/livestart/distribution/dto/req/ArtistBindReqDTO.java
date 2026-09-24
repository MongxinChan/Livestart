package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 歌迷绑定艺人推广关系请求 DTO。
 */
@Data
@Schema(description = "歌迷绑定艺人推广关系请求")
public class ArtistBindReqDTO {

    /** 艺人推广码。 */
    @NotBlank(message = "艺人推广码不能为空")
    @Schema(description = "艺人推广码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;
}
