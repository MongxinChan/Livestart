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

    @Override
    public void createStyle(StyleDO requestParam) {
        // code 全局唯一防重（如 ROCK、FOLK、JAZZ）
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
    public void updateStyle(StyleDO requestParam) {
        updateById(requestParam);
    }

    @Override
    public void deleteStyle(Long id) {
        removeById(id);
    }
}
