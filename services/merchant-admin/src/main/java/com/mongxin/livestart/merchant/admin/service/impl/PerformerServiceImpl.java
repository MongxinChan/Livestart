package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import org.springframework.stereotype.Service;

@Service
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {
}
