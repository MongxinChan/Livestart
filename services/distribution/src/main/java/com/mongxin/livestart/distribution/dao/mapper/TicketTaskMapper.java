package com.mongxin.livestart.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.distribution.dao.entity.TicketTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门票批量推送任务 Mapper 接口
 */
@Mapper
public interface TicketTaskMapper extends BaseMapper<TicketTaskDO> {
}
