package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-admin/venue")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping("/create")
    public Result<Void> createVenue(@RequestBody VenueDO requestParam) {
        venueService.createVenue(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<VenueDO>> listVenues() {
        return Results.success(venueService.listAllVenues());
    }

    /**
     * 分页查询场馆列表
     *
     * @param current 当前页码
     * @param size    每页数量
     * @param city    按城市筛选（可选）
     */
    @GetMapping("/page")
    public Result<IPage<VenueDO>> pageQueryVenues(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String city) {
        return Results.success(venueService.pageQueryVenues(new Page<>(current, size), city));
    }

    /**
     * 根据 ID 查询场馆详情
     */
    @GetMapping("/{id}")
    public Result<VenueDO> getVenue(@PathVariable("id") Long id) {
        return Results.success(venueService.getVenueById(id));
    }

    @PutMapping("/update")
    public Result<Void> updateVenue(@RequestBody VenueDO requestParam) {
        venueService.updateVenue(requestParam);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteVenue(@PathVariable("id") Long id) {
        venueService.deleteVenue(id);
        return Results.success();
    }
}
