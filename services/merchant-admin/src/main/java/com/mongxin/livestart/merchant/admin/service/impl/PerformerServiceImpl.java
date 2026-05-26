package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 艺人/乐队服务实现层
 */
@Service
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {

    @Override
    public void createPerformer(PerformerDO requestParam) {
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .eq(PerformerDO::getName, requestParam.getName());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("艺人/乐队名称已存在，请勿重复录入");
        }
        save(requestParam);
    }

    @Override
    public List<PerformerDO> listAllPerformers() {
        return list();
    }

    @Override
    public IPage<PerformerDO> pageQueryPerformers(Page<PerformerDO> page, String name) {
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .like(StrUtil.isNotBlank(name), PerformerDO::getName, name)
                .orderByDesc(PerformerDO::getId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public PerformerDO getPerformerById(Long id) {
        return getById(id);
    }

    @Override
    public void updatePerformer(PerformerDO requestParam) {
        updateById(requestParam);
    }

    @Override
    public void deletePerformer(Long id) {
        removeById(id);
    }
}
