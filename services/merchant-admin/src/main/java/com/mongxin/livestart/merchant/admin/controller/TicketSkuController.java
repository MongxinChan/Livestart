package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmit;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuIncreaseStockReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.TicketSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 票种/档位管理控制层
 */
@RestController
@RequestMapping("/api/merchant-admin/ticket-sku")
@RequiredArgsConstructor
@Tag(name = "票种/档位管理")
public class TicketSkuController {

    private final TicketSkuService ticketSkuService;

    @Operation(summary = "创建票种")
    @NoDuplicateSubmit(message = "请勿短时间内重复创建票种")
    @PostMapping("/create")
    public Result<Void> createTicketSku(@RequestBody TicketSkuSaveReqDTO requestParam) {
        ticketSkuService.createTicketSku(requestParam);
        return Results.success();
    }

    @Operation(summary = "按演出ID查询票种列表")
    @GetMapping("/list/{eventId}")
    public Result<List<TicketSkuQueryRespDTO>> listTicketSkus(@PathVariable("eventId") Long eventId) {
        return Results.success(ticketSkuService.listByEventId(eventId));
    }

    @Operation(summary = "分页查询票种列表")
    @GetMapping("/page")
    public Result<IPage<TicketSkuPageQueryRespDTO>> pageQueryTicketSkus(TicketSkuPageQueryReqDTO requestParam) {
        return Results.success(ticketSkuService.pageQueryTicketSkus(requestParam));
    }

    @Operation(summary = "查询票种详情")
    @GetMapping("/{id}")
    public Result<TicketSkuQueryRespDTO> getTicketSku(@PathVariable("id") Long id) {
        return Results.success(ticketSkuService.getTicketSkuById(id));
    }

    @Operation(summary = "增发票种库存")
    @NoDuplicateSubmit(message = "请勿短时间内重复增发库存")
    @PostMapping("/increase-stock")
    public Result<Void> increaseStock(@RequestBody TicketSkuIncreaseStockReqDTO requestParam) {
        ticketSkuService.increaseStock(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除票种")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteTicketSku(@PathVariable("id") Long id) {
        ticketSkuService.deleteTicketSku(id);
        return Results.success();
    }

    @Operation(summary = "修改票种信息")
    @PutMapping("/update")
    public Result<Void> updateTicketSku(@RequestBody com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO requestParam) {
        ticketSkuService.updateTicketSku(requestParam);
        return Results.success();
    }
}
