package com.mongxin.livestart.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 演出配置持久层映射器
 * 采用演出ID作为主键关联，解耦核心表与配置表。
 */
@Mapper
public interface EventConfigMapper extends BaseMapper<EventConfigDO> {
}
