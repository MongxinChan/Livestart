package com.mongxin.livestart.search.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_performer")
public class PerformerDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long styleId;

    private String avatar;

    private String bio;

    private Integer status;

    private Date createTime;
}
