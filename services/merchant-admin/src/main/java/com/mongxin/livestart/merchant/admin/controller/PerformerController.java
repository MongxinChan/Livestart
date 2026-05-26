package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-admin/performer")
@RequiredArgsConstructor
public class PerformerController {

    private final PerformerService performerService;

    @PostMapping("/create")
    public Result<Void> createPerformer(@RequestBody PerformerDO requestParam) {
        performerService.createPerformer(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<PerformerDO>> listPerformers() {
        return Results.success(performerService.listAllPerformers());
    }

    /**
     * 分页查询艺人列表
     *
     * @param current 当前页码
     * @param size    每页数量
     * @param name    按名称模糊搜索（可选）
     */
    @GetMapping("/page")
    public Result<IPage<PerformerDO>> pageQueryPerformers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {
        return Results.success(performerService.pageQueryPerformers(new Page<>(current, size), name));
    }

    /**
     * 根据 ID 查询艺人详情
     */
    @GetMapping("/{id}")
    public Result<PerformerDO> getPerformer(@PathVariable("id") Long id) {
        return Results.success(performerService.getPerformerById(id));
    }

    @PutMapping("/update")
    public Result<Void> updatePerformer(@RequestBody PerformerDO requestParam) {
        performerService.updatePerformer(requestParam);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePerformer(@PathVariable("id") Long id) {
        performerService.deletePerformer(id);
        return Results.success();
    }
}
