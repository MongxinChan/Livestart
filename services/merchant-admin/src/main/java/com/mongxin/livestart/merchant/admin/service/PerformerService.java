package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerQueryRespDTO;

/**
 * 艺人/乐队业务逻辑层
 */
public interface PerformerService extends IService<PerformerDO> {

    /**
     * 创建艺人/乐队（名称防重）
     */
    void createPerformer(PerformerSaveReqDTO requestParam);

    /**
     * 分页查询艺人（支持按名称模糊搜索）
     */
    IPage<PerformerPageQueryRespDTO> pageQueryPerformers(PerformerPageQueryReqDTO requestParam);

    /**
     * 根据 ID 查询艺人详情
     */
    PerformerQueryRespDTO getPerformerById(Long id);

    /**
     * 修改艺人/乐队信息
     */
    void updatePerformer(PerformerSaveReqDTO requestParam);

    /**
     * 删除艺人/乐队
     */
    void deletePerformer(Long id);
}
