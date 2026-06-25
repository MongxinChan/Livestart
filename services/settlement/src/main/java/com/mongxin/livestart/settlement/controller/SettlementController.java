package com.mongxin.livestart.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.settlement.dto.resp.SettlementRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementShardRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementStatsRespDTO;
import com.mongxin.livestart.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 结算中心 Controller
 */
@Tag(name = "结算中心")
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 查询结算单列表
     */
    @Operation(summary = "结算单列表", description = "查询主办方的结算单列表，支持按演出筛选")
    @GetMapping("/list")
    public Result<IPage<SettlementRespDTO>> listSettlements(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Results.success(settlementService.pageSettlements(eventId, pageNum, pageSize));
    }

    /**
     * 查询结算单详情
     */
    @Operation(summary = "结算单详情", description = "查询指定结算单的详细信息，含票务收入、佣金、退款扣减等")
    @GetMapping("/detail/{settlementId}")
    public Result<SettlementRespDTO> getSettlementDetail(@PathVariable Long settlementId) {
        return Results.success(settlementService.getSettlementDetail(settlementId));
    }

    /**
     * 触发结算（管理端调用）
     */
    @Operation(summary = "触发结算", description = "针对已完成演出发起结算流程，计算票务收入并生成结算单")
    @PostMapping("/trigger/{eventId}")
    public Result<Void> triggerSettlement(@PathVariable Long eventId) {
        settlementService.triggerSettlement(eventId);
        return Results.success();
    }

    @Operation(summary = "触发当前可见范围结算", description = "超管结算当前全部演出，场馆管理员只结算自己场馆下的演出")
    @PostMapping("/trigger-visible")
    public Result<Void> triggerVisibleSettlements() {
        settlementService.triggerVisibleSettlements();
        return Results.success();
    }

    /**
     * 收入统计概览
     */
    @Operation(summary = "收入统计", description = "查询收入统计概览数据：总收入、总退款、净收入等")
    @GetMapping("/stats")
    public Result<SettlementStatsRespDTO> incomeStats(
            @RequestParam(required = false) Long eventId) {
        return Results.success(settlementService.getIncomeStats(eventId));
    }

    @Operation(summary = "分表结算明细", description = "查询指定演出的 16 张物理订单分表结算明细")
    @GetMapping("/shards")
    public Result<List<SettlementShardRespDTO>> settlementShards(@RequestParam(required = false) Long eventId) {
        return Results.success(settlementService.listSettlementShards(eventId));
    }
}
