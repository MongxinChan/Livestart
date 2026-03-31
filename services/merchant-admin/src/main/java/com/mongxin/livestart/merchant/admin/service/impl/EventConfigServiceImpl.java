package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventConfigMapper;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import org.springframework.stereotype.Service;

@Service
public class EventConfigServiceImpl extends ServiceImpl<EventConfigMapper, EventConfigDO> implements EventConfigService {
}
