package com.mongxin.livestart.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.settlement.common.biz.user.UserContext;
import com.mongxin.livestart.settlement.dao.entity.SettlementDO;
import com.mongxin.livestart.settlement.dao.mapper.SettlementMapper;
import com.mongxin.livestart.settlement.dto.resp.SettlementRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementShardRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementStatsRespDTO;
import com.mongxin.livestart.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.0500");
    private static final int SHARDING_TABLES_COUNT = 16;
    private static final List<String> ORDER_DATABASES = Arrays.asList("ds_order_0", "ds_order_1");
    private static final int USER_TYPE_VENUE_ADMIN = 3;
    private static final int USER_TYPE_SUPER_ADMIN = 4;

    private final SettlementMapper settlementMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public IPage<SettlementRespDTO> pageSettlements(Long eventId, Integer pageNum, Integer pageSize) {
        Set<Long> visibleEventIds = resolveVisibleEventIds();
        if (visibleEventIds != null && visibleEventIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0L);
        }

        Page<SettlementDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SettlementDO> queryWrapper = Wrappers.lambdaQuery(SettlementDO.class);
        if (visibleEventIds != null) {
            queryWrapper.in(SettlementDO::getEventId, visibleEventIds);
        }
        if (eventId != null) {
            queryWrapper.eq(SettlementDO::getEventId, eventId);
        }
        queryWrapper.orderByDesc(SettlementDO::getEventId);

        IPage<SettlementDO> doPage = settlementMapper.selectPage(page, queryWrapper);
        IPage<SettlementRespDTO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(doPage.getTotal());
        resultPage.setPages(doPage.getPages());
        resultPage.setRecords(doPage.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, SettlementRespDTO.class))
                .collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public SettlementRespDTO getSettlementDetail(Long settlementId) {
        SettlementDO settlementDO = settlementMapper.selectById(settlementId);
        if (settlementDO == null) {
            throw new ServiceException("结算单不存在");
        }
        ensureVisibleEvent(settlementDO.getEventId());
        return BeanUtil.copyProperties(settlementDO, SettlementRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerSettlement(Long eventId) {
        ensureVisibleEvent(eventId);
        String eventTitle = queryEventTitle(eventId);
        List<SettlementShardRespDTO> shardRows = allocateShardAmounts(
                aggregateLogicalShards(scanPhysicalSettlementShards(Collections.singleton(eventId)))
        );

        int totalTickets = shardRows.stream().mapToInt(SettlementShardRespDTO::getTotalTickets).sum();
        BigDecimal totalSalesAmount = shardRows.stream().map(SettlementShardRespDTO::getTotalSalesAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commissionAmount = shardRows.stream().map(SettlementShardRespDTO::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settlementAmount = shardRows.stream().map(SettlementShardRespDTO::getSettlementAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        SettlementDO existing = settlementMapper.selectOne(Wrappers.lambdaQuery(SettlementDO.class).eq(SettlementDO::getEventId, eventId));
        SettlementDO settlement = SettlementDO.builder()
                .eventId(eventId)
                .eventTitle(eventTitle)
                .totalTickets(totalTickets)
                .totalSalesAmount(totalSalesAmount)
                .commissionRate(DEFAULT_COMMISSION_RATE)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .status(1)
                .build();

        if (existing != null) {
            settlement.setId(existing.getId());
            settlementMapper.updateById(settlement);
        } else {
            settlementMapper.insert(settlement);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerVisibleSettlements() {
        Set<Long> visibleEventIds = resolveVisibleEventIds();
        if (visibleEventIds == null) {
            visibleEventIds = queryAllEventIds();
        }
        if (visibleEventIds.isEmpty()) {
            throw new ServiceException("当前账号暂无可结算的演出");
        }
        for (Long eventId : visibleEventIds) {
            triggerSettlement(eventId);
        }
    }

    @Override
    public SettlementStatsRespDTO getIncomeStats(Long eventId) {
        Set<Long> visibleEventIds = resolveVisibleEventIds();
        if (visibleEventIds != null && visibleEventIds.isEmpty()) {
            return emptyStats();
        }

        LambdaQueryWrapper<SettlementDO> queryWrapper = Wrappers.lambdaQuery(SettlementDO.class);
        if (visibleEventIds != null) {
            queryWrapper.in(SettlementDO::getEventId, visibleEventIds);
        }
        if (eventId != null) {
            queryWrapper.eq(SettlementDO::getEventId, eventId);
        }
        List<SettlementDO> list = settlementMapper.selectList(queryWrapper);
        return SettlementStatsRespDTO.builder()
                .totalEvents(list.size())
                .totalTickets(list.stream().mapToInt(SettlementDO::getTotalTickets).sum())
                .grossRevenue(list.stream().map(SettlementDO::getTotalSalesAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalCommission(list.stream().map(SettlementDO::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .netSettlement(list.stream().map(SettlementDO::getSettlementAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }

    @Override
    public List<SettlementShardRespDTO> listSettlementShards(Long eventId) {
        if (eventId != null) {
            ensureVisibleEvent(eventId);
            return allocateShardAmounts(aggregateLogicalShards(scanPhysicalSettlementShards(Collections.singleton(eventId))));
        }

        Set<Long> visibleEventIds = resolveVisibleEventIds();
        if (visibleEventIds == null) {
            visibleEventIds = queryAllEventIds();
        }
        if (visibleEventIds.isEmpty()) {
            return Collections.emptyList();
        }
        return allocateShardAmounts(aggregateLogicalShards(scanPhysicalSettlementShards(visibleEventIds)));
    }

    private Set<Long> resolveVisibleEventIds() {
        Integer userType = UserContext.getUserType();
        String userId = UserContext.getUserId();
        if (userType == null || StrUtil.isBlank(userId)) {
            return Collections.emptySet();
        }
        if (userType == USER_TYPE_SUPER_ADMIN) {
            return null;
        }
        if (userType != USER_TYPE_VENUE_ADMIN) {
            return Collections.emptySet();
        }

        List<Long> venueIds = jdbcTemplate.queryForList(
                "SELECT id FROM live_start.t_venue WHERE owner_user_id = ?",
                Long.class,
                Long.valueOf(userId)
        );
        if (CollUtil.isEmpty(venueIds)) {
            return Collections.emptySet();
        }
        String inSql = venueIds.stream().map(v -> "?").collect(Collectors.joining(","));
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT id FROM live_start.t_event WHERE venue_id IN (" + inSql + ") ORDER BY id DESC",
                Long.class,
                venueIds.toArray()
        ));
    }

    private Set<Long> queryAllEventIds() {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT id FROM live_start.t_event ORDER BY id DESC",
                Long.class
        ));
    }

    private void ensureVisibleEvent(Long eventId) {
        Set<Long> visibleEventIds = resolveVisibleEventIds();
        if (visibleEventIds == null) {
            return;
        }
        if (!visibleEventIds.contains(eventId)) {
            throw new ServiceException("当前账号无权查看该演出结算");
        }
    }

    private SettlementStatsRespDTO emptyStats() {
        return SettlementStatsRespDTO.builder()
                .totalEvents(0)
                .totalTickets(0)
                .grossRevenue(BigDecimal.ZERO)
                .totalCommission(BigDecimal.ZERO)
                .netSettlement(BigDecimal.ZERO)
                .build();
    }

    private String queryEventTitle(Long eventId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT title FROM live_start.t_event WHERE id = ?",
                    String.class,
                    eventId
            );
        } catch (Exception ex) {
            throw new ServiceException("演出项目不存在，无法结算");
        }
    }

    private List<SettlementShardRespDTO> scanPhysicalSettlementShards(Set<Long> eventIds) {
        if (CollUtil.isEmpty(eventIds)) {
            return Collections.emptyList();
        }
        return ORDER_DATABASES.stream()
                .flatMap(database -> scanDatabaseShards(database, eventIds).stream())
                .collect(Collectors.toList());
    }

    private List<SettlementShardRespDTO> aggregateLogicalShards(List<SettlementShardRespDTO> physicalShards) {
        return physicalShards.stream()
                .collect(Collectors.groupingBy(
                        SettlementShardRespDTO::getShardIndex,
                        Collectors.collectingAndThen(Collectors.toList(), this::mergeShardGroup)
                ))
                .values().stream()
                .sorted((left, right) -> Integer.compare(left.getShardIndex(), right.getShardIndex()))
                .collect(Collectors.toList());
    }

    private List<SettlementShardRespDTO> allocateShardAmounts(List<SettlementShardRespDTO> shards) {
        if (shards.isEmpty()) {
            return shards;
        }

        BigDecimal totalRevenue = shards.stream().map(SettlementShardRespDTO::getTotalSalesAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal targetCommission = totalRevenue.multiply(DEFAULT_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        long targetCommissionCents = targetCommission.movePointRight(2).longValueExact();

        List<ShardAllocation> allocations = shards.stream()
                .map(shard -> {
                    BigDecimal rawCommission = shard.getTotalSalesAmount().multiply(DEFAULT_COMMISSION_RATE);
                    BigDecimal floorCommission = rawCommission.setScale(2, RoundingMode.DOWN);
                    return new ShardAllocation(shard, floorCommission.movePointRight(2).longValueExact(), rawCommission.subtract(floorCommission));
                })
                .collect(Collectors.toList());

        long allocatedCents = allocations.stream().mapToLong(ShardAllocation::getFloorCents).sum();
        long remainingCents = targetCommissionCents - allocatedCents;
        allocations.sort((a, b) -> {
            int compare = b.getRemainder().compareTo(a.getRemainder());
            return compare != 0 ? compare : Integer.compare(a.getShard().getShardIndex(), b.getShard().getShardIndex());
        });
        for (int i = 0; i < remainingCents && i < allocations.size(); i++) {
            allocations.get(i).addExtraCent();
        }

        allocations.sort((a, b) -> Integer.compare(a.getShard().getShardIndex(), b.getShard().getShardIndex()));
        for (ShardAllocation allocation : allocations) {
            BigDecimal commissionAmount = allocation.getCommissionAmount();
            BigDecimal settlementAmount = allocation.getShard().getTotalSalesAmount().subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);
            allocation.getShard().setCommissionAmount(commissionAmount);
            allocation.getShard().setSettlementAmount(settlementAmount);
        }
        return shards;
    }

    private SettlementShardRespDTO mergeShardGroup(List<SettlementShardRespDTO> shardGroup) {
        SettlementShardRespDTO first = shardGroup.get(0);
        return SettlementShardRespDTO.builder()
                .shardIndex(first.getShardIndex())
                .tableName("t_order_item_" + first.getShardIndex())
                .totalTickets(shardGroup.stream().mapToInt(SettlementShardRespDTO::getTotalTickets).sum())
                .totalSalesAmount(shardGroup.stream().map(SettlementShardRespDTO::getTotalSalesAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .commissionAmount(shardGroup.stream().map(SettlementShardRespDTO::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .settlementAmount(shardGroup.stream().map(SettlementShardRespDTO::getSettlementAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }

    private List<SettlementShardRespDTO> scanDatabaseShards(String database, Set<Long> eventIds) {
        return Collections.unmodifiableList(
                java.util.stream.IntStream.range(0, SHARDING_TABLES_COUNT)
                        .mapToObj(shardIndex -> scanSingleShard(database, shardIndex, eventIds))
                        .collect(Collectors.toList())
        );
    }

    private SettlementShardRespDTO scanSingleShard(String database, int shardIndex, Set<Long> eventIds) {
        String inSql = eventIds.size() == 1
                ? "oi.event_id = ?"
                : "oi.event_id IN (" + eventIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        String queryOrdersSql = String.format("""
                SELECT COUNT(*) AS ticket_count,
                       COALESCE(SUM(sku.selling_price), 0) AS revenue
                FROM `%s`.t_order_item_%d oi
                JOIN live_start.t_ticket_sku sku ON oi.sku_id = sku.id
                JOIN `%s`.t_order_%d o ON oi.order_no = o.order_no
                WHERE %s AND (o.status = 1 OR o.status = 2)
                """, database, shardIndex, database, shardIndex, inSql);

        try {
            SettlementAggRow row = jdbcTemplate.queryForObject(queryOrdersSql, (ResultSet rs, int rowNum) -> {
                SettlementAggRow aggRow = new SettlementAggRow();
                aggRow.ticketCount = rs.getLong("ticket_count");
                aggRow.revenue = rs.getBigDecimal("revenue");
                return aggRow;
            }, eventIds.toArray());

            long ticketCount = row == null ? 0L : row.ticketCount;
            BigDecimal revenue = row == null || row.revenue == null ? BigDecimal.ZERO : row.revenue.setScale(2, RoundingMode.HALF_UP);
            BigDecimal commissionAmount = revenue.multiply(DEFAULT_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal settlementAmount = revenue.subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);
            return SettlementShardRespDTO.builder()
                    .shardIndex(shardIndex)
                    .tableName(database + ".t_order_item_" + shardIndex)
                    .totalTickets(Math.toIntExact(ticketCount))
                    .totalSalesAmount(revenue)
                    .commissionAmount(commissionAmount)
                    .settlementAmount(settlementAmount)
                    .build();
        } catch (Exception ex) {
            throw new ServiceException("扫描分库分表核对资金失败，结算终止");
        }
    }

    private static final class SettlementAggRow {
        private long ticketCount;
        private BigDecimal revenue;
    }

    private static final class ShardAllocation {
        private final SettlementShardRespDTO shard;
        private long floorCents;
        private final BigDecimal remainder;

        private ShardAllocation(SettlementShardRespDTO shard, long floorCents, BigDecimal remainder) {
            this.shard = shard;
            this.floorCents = floorCents;
            this.remainder = remainder;
        }

        private SettlementShardRespDTO getShard() {
            return shard;
        }

        private long getFloorCents() {
            return floorCents;
        }

        private BigDecimal getRemainder() {
            return remainder;
        }

        private void addExtraCent() {
            floorCents += 1;
        }

        private BigDecimal getCommissionAmount() {
            return BigDecimal.valueOf(floorCents, 2);
        }
    }
}
