package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.dto.resp.SaleStagePreviewRespDTO;
import com.mongxin.livestart.distribution.dto.resp.SaleStageRespDTO;

import java.util.List;

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

    SaleStagePreviewRespDTO getNextSaleStage(Long eventId);

    /**
     * 查询分销演出的全部开售阶段。
     *
     * @param eventId 分销演出 ID
     * @return 按阶段序号升序排列的阶段列表
     */
    List<SaleStageRespDTO> listSaleStages(Long eventId);
}
