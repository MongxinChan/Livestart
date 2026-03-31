package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;

import java.util.List;

public interface VenueService extends IService<VenueDO> {

    /**
     * 创建场馆（名称+城市防重）
     */
    void createVenue(VenueDO requestParam);

    /**
     * 查询全部场馆
     */
    List<VenueDO> listAllVenues();

    /**
     * 修改场馆信息（名称、城市、详细地址、总容纳人数）
     */
    void updateVenue(VenueDO requestParam);

    /**
     * 删除场馆
     */
    void deleteVenue(Long id);
}
