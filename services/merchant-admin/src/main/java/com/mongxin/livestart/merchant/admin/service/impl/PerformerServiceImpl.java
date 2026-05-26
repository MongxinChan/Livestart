package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import org.springframework.stereotype.Service;

/**
 * 艺人/乐队服务实现层
 */
@Service
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {

    @Override
    public void createPerformer(PerformerSaveReqDTO requestParam) {
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .eq(PerformerDO::getName, requestParam.getName());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("艺人/乐队名称已存在，请勿重复录入");
        }
        PerformerDO performerDO = BeanUtil.toBean(requestParam, PerformerDO.class);
        save(performerDO);
    }

    @Override
    public IPage<PerformerPageQueryRespDTO> pageQueryPerformers(PerformerPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .like(StrUtil.isNotBlank(requestParam.getName()), PerformerDO::getName, requestParam.getName())
                .orderByDesc(PerformerDO::getId);
        IPage<PerformerDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, PerformerPageQueryRespDTO.class));
    }

    @Override
    public PerformerQueryRespDTO getPerformerById(Long id) {
        PerformerDO performerDO = getById(id);
        return BeanUtil.toBean(performerDO, PerformerQueryRespDTO.class);
    }

    @Override
    public void updatePerformer(PerformerSaveReqDTO requestParam) {
        PerformerDO performerDO = BeanUtil.toBean(requestParam, PerformerDO.class);
        updateById(performerDO);
    }

    @Override
    public void deletePerformer(Long id) {
        removeById(id);
    }
}
