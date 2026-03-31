package com.mongxin.livestart.merchant.admin.service;

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
     * 删除票种
     */
    void deleteTicketSku(Long id);
}
