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

    @Override
    public void createTicketSku(TicketSkuDO requestParam) {
        // 关键：初始化剩余库存 = 总库存，确保首次录入状态一致
        requestParam.setRemainingStock(requestParam.getTotalStock());
        save(requestParam);
    }

    @Override
    public List<TicketSkuDO> listByEventId(Long eventId) {
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(TicketSkuDO::getEventId, eventId);
        return list(queryWrapper);
    }

    @Override
    public void deleteTicketSku(Long id) {
        removeById(id);
    }
}
