package com.mongxin.harmony.admin.dto.resp;

import lombok.Data;

/**
 * 短链接分组返回实际对象
 */
@Data
public class ShortLinkGroupRespDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建分组用户名
     */
    private String username;

    /**
     * 分组排序
     */
    private Integer sortOrder;

    /**
     * 分组下短链接的数量
     */
    private Integer shortLinkCount;
}
