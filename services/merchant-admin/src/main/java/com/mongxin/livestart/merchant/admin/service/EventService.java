package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 分页查询演出列表（支持按状态筛选）
     */
    IPage<EventDO> pageQueryEvents(Page<EventDO> page, Integer status);

    /**
     * 根据 ID 查询演出详情
     */
    EventDO getEventById(Long id);

    /**
     * 修改演出信息（标题、开始时间、海报地址、status 等）
     */
    void updateEvent(EventDO requestParam);

    /**
     * 删除演出及其关联的演出配置
     */
    void deleteEvent(Long id);

    /**
     * 演出上架（预售 → 在售）
     */
    void publishEvent(Long id);

    /**
     * 演出下架（在售 → 下架）
     */
    void shelveEvent(Long id);

    /**
     * 终止演出售票（任意状态 → 下架，不可逆）
     */
    void terminateEvent(Long id);
}
