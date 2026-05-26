package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dto.req.VenuePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenueSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenuePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenueQueryRespDTO;

/**
 * 场馆业务逻辑层
 */
public interface VenueService extends IService<VenueDO> {

    /**
     * 创建场馆（名称+城市防重）
     *
     * @param requestParam 创建参数
     */
    void createVenue(VenueSaveReqDTO requestParam);

    /**
     * 分页查询场馆（支持按城市筛选）
     *
     * @param requestParam 分页查询参数
     * @return 场馆分页数据
     */
    IPage<VenuePageQueryRespDTO> pageQueryVenues(VenuePageQueryReqDTO requestParam);

    /**
     * 根据 ID 查询场馆详情
     *
     * @param id 场馆ID
     * @return 场馆详情
     */
    VenueQueryRespDTO getVenueById(Long id);

    /**
     * 修改场馆信息
     *
     * @param requestParam 修改参数
     */
    void updateVenue(VenueSaveReqDTO requestParam);

    /**
     * 删除场馆
     *
     * @param id 场馆ID
     */
    void deleteVenue(Long id);
}
