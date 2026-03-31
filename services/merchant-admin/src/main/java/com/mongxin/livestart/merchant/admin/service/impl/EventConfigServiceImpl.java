package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventConfigMapper;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import org.springframework.stereotype.Service;

/**
 * 演出配置服务实现层
 */
@Service
public class EventConfigServiceImpl extends ServiceImpl<EventConfigMapper, EventConfigDO> implements EventConfigService {

    @Override
    public void saveOrUpdateConfig(EventConfigDO requestParam) {
        // 共享主键：eventId 即为主键，存在则更新，不存在则新建
        saveOrUpdate(requestParam);
    }

    @Override
    public EventConfigDO getByEventId(Long eventId) {
        return getById(eventId);
    }
}
