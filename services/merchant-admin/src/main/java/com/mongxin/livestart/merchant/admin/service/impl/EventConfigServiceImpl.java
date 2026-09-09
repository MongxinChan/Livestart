package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.entity.RefundPolicyDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventConfigMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.RefundPolicyMapper;
import com.mongxin.livestart.merchant.admin.dto.req.EventConfigUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventConfigQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 演出配置服务实现层
 */
@Service
@RequiredArgsConstructor
public class EventConfigServiceImpl extends ServiceImpl<EventConfigMapper, EventConfigDO> implements EventConfigService {

    private static final int MAX_DEADLINE_HOURS = 24 * 365;
    private final RefundPolicyMapper refundPolicyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateConfig(EventConfigUpdateReqDTO requestParam) {
        validateRefundPolicy(requestParam);
        EventConfigDO configDO = BeanUtil.toBean(requestParam, EventConfigDO.class);
        // 关键设计：eventId 直接作为 config 表的主键 (Shared Primary Key)
        // 存在则更新，不存在则录入，保障演出与其配置的高内聚性
        saveOrUpdate(configDO);
        saveOrUpdateRefundPolicy(requestParam);
    }

    @Override
    public EventConfigQueryRespDTO getConfigByEventId(Long eventId) {
        EventConfigDO configDO = getById(eventId);
        return BeanUtil.toBean(configDO, EventConfigQueryRespDTO.class);
    }

    @Override
    public EventConfigDO getByEventId(Long eventId) {
        return getById(eventId);
    }

    private void validateRefundPolicy(EventConfigUpdateReqDTO request) {
        if (request.getEventId() == null) {
            throw new ClientException("演出ID不能为空");
        }
        Integer type = request.getRefundPolicyType();
        if (type == null || type < 0 || type > 2) {
            throw new ClientException("退票政策类型不合法");
        }
        if (type == 0) {
            return;
        }
        Integer tier1 = request.getTier1FreeRefundHours();
        if (!validDeadline(tier1)) {
            throw new ClientException("全额退款截止时间必须在1到8760小时之间");
        }
        if (type == 1) {
            return;
        }
        Integer tier2 = request.getTier2PartialRefundHours();
        if (!validDeadline(tier2)) {
            throw new ClientException("部分退款截止时间必须在1到8760小时之间");
        }
        if (tier1 < tier2) {
            throw new ClientException("全额退款截止时间不能小于部分退款截止时间");
        }
        BigDecimal feeRate = request.getTier2RefundFeeRate();
        if (feeRate == null || feeRate.compareTo(BigDecimal.ZERO) < 0
                || feeRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new ClientException("部分退款手续费比例必须大于等于0且小于1");
        }
    }

    private boolean validDeadline(Integer hours) {
        return hours != null && hours > 0 && hours <= MAX_DEADLINE_HOURS;
    }

    private void saveOrUpdateRefundPolicy(EventConfigUpdateReqDTO request) {
        RefundPolicyDO policy = refundPolicyMapper.selectOne(Wrappers.lambdaQuery(RefundPolicyDO.class)
                .eq(RefundPolicyDO::getEventId, request.getEventId()));
        boolean isNew = policy == null;
        if (isNew) {
            policy = new RefundPolicyDO();
            policy.setEventId(request.getEventId());
            policy.setCreateTime(new Date());
        }

        int type = request.getRefundPolicyType();
        policy.setIsAllowRefund(type == 0 ? 0 : 1);
        if (type == 1) {
            policy.setTier1DeadlineHours(request.getTier1FreeRefundHours());
            policy.setTier2DeadlineHours(request.getTier1FreeRefundHours());
            policy.setTier2RefundFeeRate(BigDecimal.ZERO);
            policy.setPolicyDesc("开演前" + request.getTier1FreeRefundHours() + "小时可全额退款");
        } else if (type == 2) {
            policy.setTier1DeadlineHours(request.getTier1FreeRefundHours());
            policy.setTier2DeadlineHours(request.getTier2PartialRefundHours());
            policy.setTier2RefundFeeRate(request.getTier2RefundFeeRate());
            policy.setPolicyDesc("按开演时间执行阶梯退款");
        } else {
            policy.setTier1DeadlineHours(null);
            policy.setTier2DeadlineHours(null);
            policy.setTier2RefundFeeRate(null);
            policy.setPolicyDesc("不可退款");
        }
        policy.setUpdateTime(new Date());

        if (isNew) {
            refundPolicyMapper.insert(policy);
        } else {
            refundPolicyMapper.updateById(policy);
        }
    }
}
