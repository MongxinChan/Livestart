package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.dto.req.VenuePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenueSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenuePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenueQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import org.springframework.stereotype.Service;

/**
 * 场馆服务实现层
 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueDO> implements VenueService {

    @Override
    public void createVenue(VenueSaveReqDTO requestParam) {
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(VenueDO::getName, requestParam.getName())
                .eq(VenueDO::getCity, requestParam.getCity());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("该城市下已存在同名场馆，请勿重复录入");
        }
        VenueDO venueDO = BeanUtil.toBean(requestParam, VenueDO.class);
        save(venueDO);
    }

    @Override
    public IPage<VenuePageQueryRespDTO> pageQueryVenues(VenuePageQueryReqDTO requestParam) {
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(StrUtil.isNotBlank(requestParam.getCity()), VenueDO::getCity, requestParam.getCity())
                .orderByAsc(VenueDO::getCity)
                .orderByDesc(VenueDO::getId);
        IPage<VenueDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, VenuePageQueryRespDTO.class));
    }

    @Override
    public VenueQueryRespDTO getVenueById(Long id) {
        VenueDO venueDO = getById(id);
        return BeanUtil.toBean(venueDO, VenueQueryRespDTO.class);
    }

    @Override
    public void updateVenue(VenueSaveReqDTO requestParam) {
        VenueDO venueDO = BeanUtil.toBean(requestParam, VenueDO.class);
        updateById(venueDO);
    }

    @Override
    public void deleteVenue(Long id) {
        removeById(id);
    }
}
