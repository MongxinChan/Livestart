package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.dto.req.StylePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.StyleSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StylePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StyleQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.StyleService;
import org.springframework.stereotype.Service;

/**
 * 音乐风格服务实现层
 */
@Service
public class StyleServiceImpl extends ServiceImpl<StyleMapper, StyleDO> implements StyleService {

    @Override
    public void createStyle(StyleSaveReqDTO requestParam) {
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .eq(StyleDO::getCode, requestParam.getCode());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("风格代码 [" + requestParam.getCode() + "] 已存在，请勿重复录入");
        }
        StyleDO styleDO = BeanUtil.toBean(requestParam, StyleDO.class);
        save(styleDO);
    }

    @Override
    public IPage<StylePageQueryRespDTO> pageQueryStyles(StylePageQueryReqDTO requestParam) {
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .like(StrUtil.isNotBlank(requestParam.getName()), StyleDO::getName, requestParam.getName())
                .orderByDesc(StyleDO::getId);
        IPage<StyleDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, StylePageQueryRespDTO.class));
    }

    @Override
    public StyleQueryRespDTO getStyleById(Long id) {
        StyleDO styleDO = getById(id);
        return BeanUtil.toBean(styleDO, StyleQueryRespDTO.class);
    }

    @Override
    public void updateStyle(StyleSaveReqDTO requestParam) {
        StyleDO styleDO = BeanUtil.toBean(requestParam, StyleDO.class);
        updateById(styleDO);
    }

    @Override
    public void deleteStyle(Long id) {
        removeById(id);
    }
}
