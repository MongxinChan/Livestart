package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dto.req.EventConfigUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventConfigQueryRespDTO;

/**
 * 演出配置业务逻辑层
 */
public interface EventConfigService extends IService<EventConfigDO> {

    /**
     * 保存或更新演出配置
     *
     * @param requestParam 更新参数
     */
    void saveOrUpdateConfig(EventConfigUpdateReqDTO requestParam);

    /**
     * 按演出ID查询演出配置（返回 DTO）
     *
     * @param eventId 演出ID
     * @return 演出配置详情
     */
    EventConfigQueryRespDTO getConfigByEventId(Long eventId);

    /**
     * 按演出ID查询演出配置（返回 DO，供内部调用）
     *
     * @param eventId 演出ID
     * @return 演出配置 DO
     */
    EventConfigDO getByEventId(Long eventId);
}
