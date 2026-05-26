package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dto.req.EventPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventQueryRespDTO;

/**
 * 演出业务逻辑层
 */
public interface EventService extends IService<EventDO> {

    /**
     * 创建演出，并级联初始化演出配置（EventConfig）
     *
     * @param requestParam 创建参数
     */
    void createEvent(EventSaveReqDTO requestParam);

    /**
     * 分页查询演出列表（支持按状态筛选）
     *
     * @param requestParam 分页查询参数
     * @return 演出分页数据
     */
    IPage<EventPageQueryRespDTO> pageQueryEvents(EventPageQueryReqDTO requestParam);

    /**
     * 根据 ID 查询演出详情
     *
     * @param id 演出ID
     * @return 演出详情
     */
    EventQueryRespDTO getEventById(Long id);

    /**
     * 修改演出信息（标题、开始时间、海报地址、status 等）
     *
     * @param requestParam 修改参数
     */
    void updateEvent(EventUpdateReqDTO requestParam);

    /**
     * 删除演出及其关联的演出配置
     *
     * @param id 演出ID
     */
    void deleteEvent(Long id);

    /**
     * 演出上架（预售 → 在售）
     *
     * @param id 演出ID
     */
    void publishEvent(Long id);

    /**
     * 演出下架（在售 → 下架）
     *
     * @param id 演出ID
     */
    void shelveEvent(Long id);

    /**
     * 终止演出售票（任意状态 → 下架，不可逆）
     *
     * @param id 演出ID
     */
    void terminateEvent(Long id);
}
