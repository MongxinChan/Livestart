package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.service.StyleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 音乐风格服务实现层
 */
@Service
public class StyleServiceImpl extends ServiceImpl<StyleMapper, StyleDO> implements StyleService {

    /**
     * 创建音乐风格
     *
     * @param requestParam 风格创建请求参数
     */
    @Override
    public void createStyle(StyleDO requestParam) {
        // 标识符唯一性校验：如 ROCK, FOLK 等代码在系统中必须唯一，确保分类系统不紊乱
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .eq(StyleDO::getCode, requestParam.getCode());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("风格代码 [" + requestParam.getCode() + "] 已存在，请勿重复录入");
        }
        save(requestParam);
    }

    /**
     * 获取所有风格列表
     *
     * @return 风格列表
     */
    @Override
    public List<StyleDO> listAllStyles() {
        return list();
    }

    /**
     * 更新风格信息
     *
     * @param requestParam 风格更新请求参数
     */
    @Override
    public void updateStyle(StyleDO requestParam) {
        updateById(requestParam);
    }

    /**
     * 删除风格
     *
     * @param id 风格ID
     */
    @Override
    public void deleteStyle(Long id) {
        removeById(id);
    }
}
