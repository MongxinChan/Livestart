package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;

import java.util.List;

public interface StyleService extends IService<StyleDO> {

    /**
     * 创建风格（code 防重）
     */
    void createStyle(StyleDO requestParam);

    /**
     * 查询全部风格
     */
    List<StyleDO> listAllStyles();

    /**
     * 分页查询风格（支持按名称模糊搜索）
     */
    IPage<StyleDO> pageQueryStyles(Page<StyleDO> page, String name);

    /**
     * 根据 ID 查询风格详情
     */
    StyleDO getStyleById(Long id);

    /**
     * 修改风格信息（名称、code、描述）
     */
    void updateStyle(StyleDO requestParam);

    /**
     * 删除风格
     */
    void deleteStyle(Long id);
}
