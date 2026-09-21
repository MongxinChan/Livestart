package com.mongxin.livestart.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 场地实体，仅用于管理端维护归属关系
 */
@Data
@TableName("t_venue")
public class VenueDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 归属场地管理员用户 ID，NULL 表示未绑定
     */
    private Long ownerUserId;
}
