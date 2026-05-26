package com.mongxin.livestart.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.distribution.dao.entity.UserTicketDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户已持门票 Mapper 接口
 */
@Mapper
public interface UserTicketMapper extends BaseMapper<UserTicketDO> {
}
