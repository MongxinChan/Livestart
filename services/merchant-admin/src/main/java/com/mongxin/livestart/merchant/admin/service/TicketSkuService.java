package com.mongxin.livestart.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;

import java.util.List;

public interface TicketSkuService extends IService<TicketSkuDO> {

    /**
     * 创建票种（自动同步 remainingStock = totalStock）
     */
    void createTicketSku(TicketSkuDO requestParam);

    /**
     * 按演出ID查询该演出下所有票种
     */
    List<TicketSkuDO> listByEventId(Long eventId);

    /**
     * 分页查询票种列表（按演出ID筛选）
     */
    IPage<TicketSkuDO> pageQueryTicketSkus(Page<TicketSkuDO> page, Long eventId);

    /**
     * 根据 ID 查询票种详情
     */
    TicketSkuDO getTicketSkuById(Long id);

    /**
     * 增发库存（原子操作：DB 增量更新 + Redis 缓存同步递增）
     */
    void increaseStock(Long skuId, Integer count);

    /**
     * 删除票种
     */
    void deleteTicketSku(Long id);
}
