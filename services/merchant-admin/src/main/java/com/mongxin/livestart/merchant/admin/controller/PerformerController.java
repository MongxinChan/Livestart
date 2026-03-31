package com.mongxin.livestart.merchant.admin.controller;

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
        performerService.save(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<PerformerDO>> listPerformers() {
        return Results.success(performerService.list());
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePerformer(@PathVariable("id") Long id) {
        performerService.removeById(id);
        return Results.success();
    }
}
