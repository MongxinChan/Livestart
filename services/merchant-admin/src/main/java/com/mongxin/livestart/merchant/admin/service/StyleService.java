package com.mongxin.livestart.merchant.admin.service;

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
     * 修改风格信息（名称、code、描述）
     */
    void updateStyle(StyleDO requestParam);

    /**
     * 删除风格
     */
    void deleteStyle(Long id);
}
