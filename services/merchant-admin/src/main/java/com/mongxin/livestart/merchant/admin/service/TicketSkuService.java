package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuIncreaseStockReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuQueryRespDTO;

import java.util.List;

/**
 * 票种/档位业务逻辑层
 */
public interface TicketSkuService extends IService<TicketSkuDO> {

    /**
     * 创建票种（自动同步 remainingStock = totalStock）
     *
     * @param requestParam 创建参数
     */
    void createTicketSku(TicketSkuSaveReqDTO requestParam);

    /**
     * 按演出ID查询该演出下所有票种
     *
     * @param eventId 演出ID
     * @return 票种列表
     */
    List<TicketSkuQueryRespDTO> listByEventId(Long eventId);

    /**
     * 分页查询票种列表（按演出ID筛选）
     *
     * @param requestParam 分页查询参数
     * @return 票种分页数据
     */
    IPage<TicketSkuPageQueryRespDTO> pageQueryTicketSkus(TicketSkuPageQueryReqDTO requestParam);

    /**
     * 根据 ID 查询票种详情
     *
     * @param id 票种ID
     * @return 票种详情
     */
    TicketSkuQueryRespDTO getTicketSkuById(Long id);

    /**
     * 增发库存（原子操作：DB 增量更新 + Redis 缓存同步递增）
     *
     * @param requestParam 增发库存参数
     */
    void increaseStock(TicketSkuIncreaseStockReqDTO requestParam);

    /**
     * 删除票种
     *
     * @param id 票种ID
     */
    void deleteTicketSku(Long id);
}
