package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dto.req.StylePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.StyleSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StylePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StyleQueryRespDTO;

/**
 * 音乐风格业务逻辑层
 */
public interface StyleService extends IService<StyleDO> {

    /**
     * 创建风格（code 防重）
     */
    void createStyle(StyleSaveReqDTO requestParam);

    /**
     * 分页查询风格（支持按名称模糊搜索）
     */
    IPage<StylePageQueryRespDTO> pageQueryStyles(StylePageQueryReqDTO requestParam);

    /**
     * 根据 ID 查询风格详情
     */
    StyleQueryRespDTO getStyleById(Long id);

    /**
     * 修改风格信息
     */
    void updateStyle(StyleSaveReqDTO requestParam);

    /**
     * 删除风格
     */
    void deleteStyle(Long id);
}
