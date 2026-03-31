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

    /**
     * 创建演出
     * 级联初始化演出配置，确保演出核心属性完整。
     *
     * @param requestParam 演出创建请求参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEvent(EventDO requestParam) {
        // 第一步：保存演出主基座记录
        save(requestParam);

        // 第二步：连坐初始化：为演出灌注默认配置业务值
        EventConfigDO defaultConfig = new EventConfigDO();
        defaultConfig.setEventId(requestParam.getId());
        
        // 策略分流：默认开启系统自动配座，高并发场景下的吞吐利器
        defaultConfig.setSelectionMode(0);
        // 安全门槛：暂不强制实名（视具体演出热度后续动态调整）
        defaultConfig.setIsVerifyRequired(0);
        // 限购锁：单账户默认限购4张，既平衡散户利益也适度打击黄牛
        defaultConfig.setMaxTicketsPerUser(4);
        
        // 退票兜底策略
        defaultConfig.setRefundPolicyType(1);
        defaultConfig.setTier1FreeRefundHours(48);
        
        // 辅助功能：候补与转赠
        defaultConfig.setIsWaitingAllowed(0);
        defaultConfig.setIsTransferable(0);
        
        eventConfigService.save(defaultConfig);
    }

    /**
     * 获取所有演出列表
     *
     * @return 演出列表
     */
    @Override
    public List<EventDO> listAllEvents() {
        return list();
    }

    /**
     * 更新演出信息
     *
     * @param requestParam 演出更新请求参数
     */
    @Override
    public void updateEvent(EventDO requestParam) {
        updateById(requestParam);
    }

    /**
     * 删除演出
     * 执行联合删除逻辑，清理关联配置数据。
     *
     * @param id 演出ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteEvent(Long id) {
        // 斩草除根：级联删除关联的演出配置
        LambdaQueryWrapper<EventConfigDO> configQuery = Wrappers.lambdaQuery(EventConfigDO.class)
                .eq(EventConfigDO::getEventId, id);
        eventConfigService.remove(configQuery);
        
        // 彻底移除演出主记录
        removeById(id);
    }
}
