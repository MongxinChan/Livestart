package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import com.mongxin.livestart.merchant.admin.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 演出服务实现层
 */
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final EventConfigService eventConfigService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEvent(EventDO requestParam) {
        // Step1：保存演出主记录
        save(requestParam);

        // Step2：级联初始化演出配置（使用合理的业务默认值）
        EventConfigDO defaultConfig = new EventConfigDO();
        defaultConfig.setEventId(requestParam.getId());
        // 默认：系统自动配座（高并发友好）
        defaultConfig.setSelectionMode(0);
        // 默认：不强制实名
        defaultConfig.setIsVerifyRequired(0);
        // 默认：单账户最多购4张
        defaultConfig.setMaxTicketsPerUser(4);
        // 默认：全额退票政策
        defaultConfig.setRefundPolicyType(1);
        // 默认：全额退票截止时间：开演前48小时
        defaultConfig.setTier1FreeRefundHours(48);
        // 默认：不开启候补
        defaultConfig.setIsWaitingAllowed(0);
        // 默认：不允许转赠
        defaultConfig.setIsTransferable(0);
        eventConfigService.save(defaultConfig);
    }

    @Override
    public List<EventDO> listAllEvents() {
        return list();
    }

    @Override
    public void updateEvent(EventDO requestParam) {
        updateById(requestParam);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteEvent(Long id) {
        // 级联删除演出配置
        LambdaQueryWrapper<EventConfigDO> configQuery = Wrappers.lambdaQuery(EventConfigDO.class)
                .eq(EventConfigDO::getEventId, id);
        eventConfigService.remove(configQuery);
        // 删除演出主记录
        removeById(id);
    }
}
