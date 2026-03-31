package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;

public interface EventConfigService extends IService<EventConfigDO> {

    /**
     * 保存或更新演出配置（根据 eventId 判断是否已存在）
     */
    void saveOrUpdateConfig(EventConfigDO requestParam);

    /**
     * 按演出ID查询演出配置
     */
    EventConfigDO getByEventId(Long eventId);
}
