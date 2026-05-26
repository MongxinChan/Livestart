package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 艺人/乐队管理控制层
 */
@RestController
@RequestMapping("/api/merchant-admin/performer")
@RequiredArgsConstructor
@Tag(name = "艺人/乐队管理")
public class PerformerController {

    private final PerformerService performerService;

    @Operation(summary = "创建艺人/乐队")
    @PostMapping("/create")
    public Result<Void> createPerformer(@RequestBody PerformerSaveReqDTO requestParam) {
        performerService.createPerformer(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询艺人列表")
    @GetMapping("/page")
    public Result<IPage<PerformerPageQueryRespDTO>> pageQueryPerformers(PerformerPageQueryReqDTO requestParam) {
        return Results.success(performerService.pageQueryPerformers(requestParam));
    }

    @Operation(summary = "查询艺人详情")
    @GetMapping("/{id}")
    public Result<PerformerQueryRespDTO> getPerformer(@PathVariable("id") Long id) {
        return Results.success(performerService.getPerformerById(id));
    }

    @Operation(summary = "修改艺人信息")
    @PutMapping("/update")
    public Result<Void> updatePerformer(@RequestBody PerformerSaveReqDTO requestParam) {
        performerService.updatePerformer(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除艺人/乐队")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePerformer(@PathVariable("id") Long id) {
        performerService.deletePerformer(id);
        return Results.success();
    }
}
