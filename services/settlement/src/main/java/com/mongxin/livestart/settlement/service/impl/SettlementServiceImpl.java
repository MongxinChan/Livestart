package com.mongxin.livestart.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.settlement.dao.entity.SettlementDO;
import com.mongxin.livestart.settlement.dao.mapper.SettlementMapper;
import com.mongxin.livestart.settlement.dto.resp.SettlementRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementStatsRespDTO;
import com.mongxin.livestart.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结算微服务业务逻辑实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementMapper settlementMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.0500"); // 5% 抽成
    private static final int SHARDING_TABLES_COUNT = 16; // 16张分表

    @Override
    public IPage<SettlementRespDTO> pageSettlements(Long eventId, Integer pageNum, Integer pageSize) {
        Page<SettlementDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SettlementDO> queryWrapper = Wrappers.lambdaQuery(SettlementDO.class);
        if (eventId != null) {
            queryWrapper.eq(SettlementDO::getEventId, eventId);
        }
        queryWrapper.orderByDesc(SettlementDO::getEventId);

        IPage<SettlementDO> doPage = settlementMapper.selectPage(page, queryWrapper);

        IPage<SettlementRespDTO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(doPage.getTotal());
        resultPage.setPages(doPage.getPages());

        if (doPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
        } else {
            List<SettlementRespDTO> list = doPage.getRecords().stream()
                    .map(item -> BeanUtil.copyProperties(item, SettlementRespDTO.class))
                    .collect(Collectors.toList());
            resultPage.setRecords(list);
        }
        return resultPage;
    }

    @Override
    public SettlementRespDTO getSettlementDetail(Long settlementId) {
        SettlementDO settlementDO = settlementMapper.selectById(settlementId);
        if (settlementDO == null) {
            throw new ServiceException("结算单不存在");
        }
        return BeanUtil.copyProperties(settlementDO, SettlementRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerSettlement(Long eventId) {
        log.info("[结算服务] 开始为演出项目 ID: {} 执行票房资金核算...", eventId);

        // 1. 查询演出标题信息
        String eventTitle;
        try {
            String queryEventSql = "SELECT title FROM live_start.t_event WHERE id = ?";
            eventTitle = jdbcTemplate.queryForObject(queryEventSql, String.class, eventId);
        } catch (Exception ex) {
            log.error("[结算服务] 未找到对应的演出，eventId={}", eventId, ex);
            throw new ServiceException("演出项目不存在，无法结算");
        }

        int totalTickets = 0;
        BigDecimal totalSalesAmount = BigDecimal.ZERO;

        // 2. 依次全表扫描分片库 ds_order.t_order_item_{0..15} 进行票房统计
        for (int i = 0; i < SHARDING_TABLES_COUNT; i++) {
            String queryOrdersSql = String.format("""
                    SELECT COUNT(*) as ticket_count, SUM(sku.selling_price) as revenue
                    FROM ds_order.t_order_item_%d oi
                    JOIN live_start.t_ticket_sku sku ON oi.sku_id = sku.id
                    JOIN ds_order.t_order_%d o ON oi.order_no = o.order_no
                    WHERE oi.event_id = ? AND (o.status = 1 OR o.status = 2)
                    """, i, i);

            try {
                Map<String, Object> result = jdbcTemplate.queryForMap(queryOrdersSql, eventId);
                Long count = (Long) result.getOrDefault("ticket_count", 0L);
                BigDecimal revenue = (BigDecimal) result.get("revenue");

                if (count != null && count > 0) {
                    totalTickets += count.intValue();
                    if (revenue != null) {
                        totalSalesAmount = totalSalesAmount.add(revenue);
                    }
                }
            } catch (Exception ex) {
                log.error("[结算服务] 扫描第 {} 个分表失败，eventId={}", i, eventId, ex);
                throw new ServiceException("扫描分库分表核对资金失败，结算终止");
            }
        }

        // 3. 计算平台费用与商户结算额
        BigDecimal commissionRate = DEFAULT_COMMISSION_RATE;
        BigDecimal commissionAmount = totalSalesAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal settlementAmount = totalSalesAmount.subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);

        // 4. 创建或更新结算单
        LambdaQueryWrapper<SettlementDO> queryWrapper = Wrappers.lambdaQuery(SettlementDO.class)
                .eq(SettlementDO::getEventId, eventId);
        SettlementDO existing = settlementMapper.selectOne(queryWrapper);

        SettlementDO settlement = SettlementDO.builder()
                .eventId(eventId)
                .eventTitle(eventTitle)
                .totalTickets(totalTickets)
                .totalSalesAmount(totalSalesAmount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .status(1) // 已结算
                .build();

        if (existing != null) {
            settlement.setId(existing.getId());
            settlementMapper.updateById(settlement);
            log.info("[结算服务] 更新结算单成功！eventId={}, totalTickets={}, totalSales={}", eventId, totalTickets, totalSalesAmount);
        } else {
            settlementMapper.insert(settlement);
            log.info("[结算服务] 新增结算单成功！eventId={}, totalTickets={}, totalSales={}", eventId, totalTickets, totalSalesAmount);
        }
    }

    @Override
    public SettlementStatsRespDTO getIncomeStats(Long eventId) {
        log.info("[结算服务] 统计总收入指标概览...");
        LambdaQueryWrapper<SettlementDO> queryWrapper = Wrappers.lambdaQuery(SettlementDO.class);
        if (eventId != null) {
            queryWrapper.eq(SettlementDO::getEventId, eventId);
        }
        List<SettlementDO> list = settlementMapper.selectList(queryWrapper);

        int totalEvents = list.size();
        int totalTickets = list.stream().mapToInt(SettlementDO::getTotalTickets).sum();
        BigDecimal grossRevenue = list.stream()
                .map(SettlementDO::getTotalSalesAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommission = list.stream()
                .map(SettlementDO::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netSettlement = list.stream()
                .map(SettlementDO::getSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SettlementStatsRespDTO.builder()
                .totalEvents(totalEvents)
                .totalTickets(totalTickets)
                .grossRevenue(grossRevenue)
                .totalCommission(totalCommission)
                .netSettlement(netSettlement)
                .build();
    }
}
