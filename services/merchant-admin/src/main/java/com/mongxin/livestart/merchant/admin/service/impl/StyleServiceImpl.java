package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Override
    public void createStyle(StyleDO requestParam) {
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .eq(StyleDO::getCode, requestParam.getCode());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("风格代码 [" + requestParam.getCode() + "] 已存在，请勿重复录入");
        }
        save(requestParam);
    }

    @Override
    public List<StyleDO> listAllStyles() {
        return list();
    }

    @Override
    public IPage<StyleDO> pageQueryStyles(Page<StyleDO> page, String name) {
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .like(StrUtil.isNotBlank(name), StyleDO::getName, name)
                .orderByDesc(StyleDO::getId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public StyleDO getStyleById(Long id) {
        return getById(id);
    }

    @Override
    public void updateStyle(StyleDO requestParam) {
        updateById(requestParam);
    }

    @Override
    public void deleteStyle(Long id) {
        removeById(id);
    }
}
