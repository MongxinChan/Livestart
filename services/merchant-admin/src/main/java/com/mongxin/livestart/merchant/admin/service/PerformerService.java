package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 分页查询艺人（支持按名称模糊搜索）
     */
    IPage<PerformerDO> pageQueryPerformers(Page<PerformerDO> page, String name);

    /**
     * 根据 ID 查询艺人详情
     */
    PerformerDO getPerformerById(Long id);

    /**
     * 修改艺人/乐队信息（名称、avatar、bio、status）
     */
    void updatePerformer(PerformerDO requestParam);

    /**
     * 删除艺人/乐队
     */
    void deletePerformer(Long id);
}
