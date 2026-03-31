package com.mongxin.livestart.merchant.admin.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.service.TicketSkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-admin/ticket-sku")
@RequiredArgsConstructor
public class TicketSkuController {

    private final TicketSkuService ticketSkuService;

    @PostMapping("/create")
    public Result<Void> createTicketSku(@RequestBody TicketSkuDO requestParam) {
        ticketSkuService.createTicketSku(requestParam);
        return Results.success();
    }

    /**
     * 按演出ID查询票种列表
     */
    @GetMapping("/list/{eventId}")
    public Result<List<TicketSkuDO>> listTicketSkus(@PathVariable("eventId") Long eventId) {
        return Results.success(ticketSkuService.listByEventId(eventId));
    }

    @PutMapping("/update")
    public Result<Void> updateTicketSku(@RequestBody TicketSkuDO requestParam) {
        ticketSkuService.updateById(requestParam);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteTicketSku(@PathVariable("id") Long id) {
        ticketSkuService.deleteTicketSku(id);
        return Results.success();
    }
}
