package com.mongxin.livestart.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 手机号到用户分片键的全局映射。
 */
@Data
@TableName("t_user_phone_mapping")
public class UserPhoneMappingDO {

    /**
     * 完整手机号，作为查找用户分片键的唯一键。
     */
    @TableId(type = IdType.INPUT)
    private String phone;

    /**
     * 用户 ID，也是用户表的分片键。
     */
    private Long userId;

    /**
     * 映射记录的创建时间。
     */
    private Date createTime;
}
