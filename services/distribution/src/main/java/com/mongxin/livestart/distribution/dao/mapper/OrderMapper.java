package com.mongxin.livestart.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.distribution.dao.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简易订单只读 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
