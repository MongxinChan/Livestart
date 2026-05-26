package com.mongxin.livestart.merchant.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dto.req.VenuePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenueSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenuePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenueQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 场馆管理控制层
 */
@RestController
@RequestMapping("/api/merchant-admin/venue")
@RequiredArgsConstructor
@Tag(name = "场馆管理")
public class VenueController {

    private final VenueService venueService;

    @Operation(summary = "创建场馆")
    @PostMapping("/create")
    public Result<Void> createVenue(@RequestBody VenueSaveReqDTO requestParam) {
        venueService.createVenue(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询场馆列表")
    @GetMapping("/page")
    public Result<IPage<VenuePageQueryRespDTO>> pageQueryVenues(VenuePageQueryReqDTO requestParam) {
        return Results.success(venueService.pageQueryVenues(requestParam));
    }

    @Operation(summary = "查询场馆详情")
    @GetMapping("/{id}")
    public Result<VenueQueryRespDTO> getVenue(@PathVariable("id") Long id) {
        return Results.success(venueService.getVenueById(id));
    }

    @Operation(summary = "修改场馆信息")
    @PutMapping("/update")
    public Result<Void> updateVenue(@RequestBody VenueSaveReqDTO requestParam) {
        venueService.updateVenue(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除场馆")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteVenue(@PathVariable("id") Long id) {
        venueService.deleteVenue(id);
        return Results.success();
    }
}
