package com.mongxin.livestart.engine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyStatsRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/engine/order/verify")
public class TicketVerifyStatsController {

    private final OrderItemMapper orderItemMapper;

    @GetMapping("/stats")
    public TicketVerifyStatsRespDTO stats(@RequestParam(required = false) Long eventId) {
        LambdaQueryWrapper<OrderItemDO> totalQuery = Wrappers.lambdaQuery(OrderItemDO.class);
        if (eventId != null) {
            totalQuery.eq(OrderItemDO::getEventId, eventId);
        }
        Long totalCount = orderItemMapper.selectCount(totalQuery);

        LambdaQueryWrapper<OrderItemDO> checkedQuery = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getIsChecked, 1);
        if (eventId != null) {
            checkedQuery.eq(OrderItemDO::getEventId, eventId);
        }
        Long checkedCount = orderItemMapper.selectCount(checkedQuery);

        TicketVerifyStatsRespDTO result = new TicketVerifyStatsRespDTO();
        result.setTotalCount(totalCount);
        result.setCheckedCount(checkedCount);
        result.setUncheckedCount(Math.max(totalCount - checkedCount, 0L));
        result.setCheckedRate(totalCount == 0L ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(checkedCount).multiply(java.math.BigDecimal.valueOf(100)).divide(java.math.BigDecimal.valueOf(totalCount), 2, java.math.RoundingMode.HALF_UP));
        return result;
    }
}
