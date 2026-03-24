package com.mongxin.livestart.admin.dto.resp;

import lombok.Data;

/**
 * 短链接分组查询
 *
 * @author Mongxin
 */
@Data
public class ShortLinkGroupCountQueryRespDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接数量
     */
    private Integer shortLinkCount;
}
