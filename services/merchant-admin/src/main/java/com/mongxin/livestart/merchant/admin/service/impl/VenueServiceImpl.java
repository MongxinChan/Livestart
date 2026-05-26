package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 场馆服务实现层
 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueDO> implements VenueService {

    @Override
    public void createVenue(VenueDO requestParam) {
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(VenueDO::getName, requestParam.getName())
                .eq(VenueDO::getCity, requestParam.getCity());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("该城市下已存在同名场馆，请勿重复录入");
        }
        save(requestParam);
    }

    @Override
    public List<VenueDO> listAllVenues() {
        return list();
    }

    @Override
    public IPage<VenueDO> pageQueryVenues(Page<VenueDO> page, String city) {
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(StrUtil.isNotBlank(city), VenueDO::getCity, city)
                .orderByDesc(VenueDO::getId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public VenueDO getVenueById(Long id) {
        return getById(id);
    }

    @Override
    public void updateVenue(VenueDO requestParam) {
        updateById(requestParam);
    }

    @Override
    public void deleteVenue(Long id) {
        removeById(id);
    }
}
