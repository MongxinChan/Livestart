package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;

import java.util.List;

public interface EventService extends IService<EventDO> {

    /**
     * 创建演出，并级联初始化演出配置（EventConfig）
     */
    void createEvent(EventDO requestParam);

    /**
     * 查询全部演出
     */
    List<EventDO> listAllEvents();

    /**
     * 修改演出信息（标题、开始时间、海报地址、status 等）
     */
    void updateEvent(EventDO requestParam);

    /**
     * 删除演出及其关联的演出配置
     */
    void deleteEvent(Long id);
}
