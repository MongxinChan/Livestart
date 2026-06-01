package com.mongxin.livestart.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.merchant.admin.dao.entity.OperationLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一操作审计日志 Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogDO> {
}
