package com.mongxin.livestart.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 票种分页查询接口请求参数
 */
@Data
@Schema(description = "票种分页查询参数")
public class TicketSkuPageQueryReqDTO extends Page {

    @Schema(description = "按演出ID筛选")
    private Long eventId;
}
