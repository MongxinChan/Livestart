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

    /**
     * 保存或更新演出配置
     * 采用共享主键策略，简化关联查询逻辑。
     *
     * @param requestParam 配置参数
     */
    @Override
    public void saveOrUpdateConfig(EventConfigDO requestParam) {
        // 关键设计：eventId 直接作为 config 表的主键 (Shared Primary Key)
        // 存在则更新，不存在则录入，保障演出与其配置的高内聚性
        saveOrUpdate(requestParam);
    }

    /**
     * 根据演出ID获取配置
     *
     * @param eventId 演出ID
     * @return 演出配置实体
     */
    @Override
    public EventConfigDO getByEventId(Long eventId) {
        return getById(eventId);
    }
}
