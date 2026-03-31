package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;

import java.util.List;

public interface PerformerService extends IService<PerformerDO> {

    /**
     * 创建艺人/乐队（名称防重）
     */
    void createPerformer(PerformerDO requestParam);

    /**
     * 查询全部艺人/乐队
     */
    List<PerformerDO> listAllPerformers();

    /**
     * 修改艺人/乐队信息（名称、avatar、bio、status）
     */
    void updatePerformer(PerformerDO requestParam);

    /**
     * 删除艺人/乐队
     */
    void deletePerformer(Long id);
}
