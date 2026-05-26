package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventConfigMapper;
import com.mongxin.livestart.merchant.admin.dto.req.EventConfigUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventConfigQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import org.springframework.stereotype.Service;

/**
 * 演出配置服务实现层
 */
@Service
public class EventConfigServiceImpl extends ServiceImpl<EventConfigMapper, EventConfigDO> implements EventConfigService {

    @Override
    public void saveOrUpdateConfig(EventConfigUpdateReqDTO requestParam) {
        EventConfigDO configDO = BeanUtil.toBean(requestParam, EventConfigDO.class);
        // 关键设计：eventId 直接作为 config 表的主键 (Shared Primary Key)
        // 存在则更新，不存在则录入，保障演出与其配置的高内聚性
        saveOrUpdate(configDO);
    }

    @Override
    public EventConfigQueryRespDTO getConfigByEventId(Long eventId) {
        EventConfigDO configDO = getById(eventId);
        return BeanUtil.toBean(configDO, EventConfigQueryRespDTO.class);
    }

    @Override
    public EventConfigDO getByEventId(Long eventId) {
        return getById(eventId);
    }
}
