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
        ticketSkuService.save(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<TicketSkuDO>> listTicketSkus() {
        return Results.success(ticketSkuService.list());
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteTicketSku(@PathVariable("id") Long id) {
        ticketSkuService.removeById(id);
        return Results.success();
    }
}
