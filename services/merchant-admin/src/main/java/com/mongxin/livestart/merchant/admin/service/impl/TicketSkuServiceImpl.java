package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.service.TicketSkuService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 票种服务实现层
 */
@Service
public class TicketSkuServiceImpl extends ServiceImpl<TicketSkuMapper, TicketSkuDO> implements TicketSkuService {

    /**
     * 创建票种（档位）
     * 自动初始化库存状态，确保数据的完整性。
     *
     * @param requestParam 票种创建请求参数
     */
    @Override
    public void createTicketSku(TicketSkuDO requestParam) {
        // 关键初始化：将剩余库存 (RemainingStock) 置为总库存 (TotalStock)
        // 这是系统库存扣减逻辑的起点，严丝合缝才能保障不超卖
        requestParam.setRemainingStock(requestParam.getTotalStock());
        save(requestParam);
    }

    /**
     * 根据演出ID查询所有关联票种
     *
     * @param eventId 演出ID
     * @return 票种列表
     */
    @Override
    public List<TicketSkuDO> listByEventId(Long eventId) {
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(TicketSkuDO::getEventId, eventId);
        return list(queryWrapper);
    }

    /**
     * 删除票种
     *
     * @param id 票种ID
     */
    @Override
    public void deleteTicketSku(Long id) {
        removeById(id);
    }
}
