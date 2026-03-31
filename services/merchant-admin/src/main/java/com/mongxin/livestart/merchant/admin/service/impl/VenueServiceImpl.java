package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

    /**
     * 创建场馆
     *
     * @param requestParam 场馆创建请求参数
     */
    @Override
    public void createVenue(VenueDO requestParam) {
        // 名称 + 城市去重锁：严防同一城市录入重复的场馆，保障数据地基稳健
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(VenueDO::getName, requestParam.getName())
                .eq(VenueDO::getCity, requestParam.getCity());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("该城市下已存在同名场馆，请勿重复录入");
        }
        save(requestParam);
    }

    /**
     * 获取所有场馆列表
     *
     * @return 场馆列表
     */
    @Override
    public List<VenueDO> listAllVenues() {
        return list();
    }

    /**
     * 更新场馆信息
     *
     * @param requestParam 场馆更新请求参数
     */
    @Override
    public void updateVenue(VenueDO requestParam) {
        updateById(requestParam);
    }

    /**
     * 删除场馆
     *
     * @param id 场馆ID
     */
    @Override
    public void deleteVenue(Long id) {
        removeById(id);
    }
}
