package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.service.EventService;
import org.springframework.stereotype.Service;

@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {
}
