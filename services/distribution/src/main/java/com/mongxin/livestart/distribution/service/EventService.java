package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;

/**
 * 演唱会演出发布及票档管理服务接口
 */
public interface EventService extends IService<EventDO> {

    /**
     * 主办方商家发布演唱会演出及其门票票档
     *
     * @param requestParam 发布演出参数
     */
    void publishEvent(EventPublishReqDTO requestParam);
}
