package com.mongxin.livestart.distribution.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门票档分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "门票票档分页查询参数")
public class TicketSkuPageQueryReqDTO extends Page<TicketSkuDO> {

    @Schema(description = "演出ID")
    private Long eventId;
}
