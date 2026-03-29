package com.mongxin.livestart.admin.dao.entity;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_user_profile")
public class UserProfileDO {
    /**
     * 关联用户ID (对应 t_user.id)
     * 注意：此处不使用自动生成，由业务层在注册时手动设置
     */
    @TableId
    private Long userId;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 性别 0:保密 1:男 2:女
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
