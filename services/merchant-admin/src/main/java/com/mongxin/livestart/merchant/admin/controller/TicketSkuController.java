package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmit;
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

    @NoDuplicateSubmit(message = "请勿短时间内重复创建票种")
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

    /**
     * 分页查询票种列表
     *
     * @param current 当前页码（默认1）
     * @param size    每页数量（默认10）
     * @param eventId 按演出ID筛选（可选）
     */
    @GetMapping("/page")
    public Result<IPage<TicketSkuDO>> pageQueryTicketSkus(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long eventId) {
        return Results.success(ticketSkuService.pageQueryTicketSkus(new Page<>(current, size), eventId));
    }

    /**
     * 根据 ID 查询票种详情
     */
    @GetMapping("/{id}")
    public Result<TicketSkuDO> getTicketSku(@PathVariable("id") Long id) {
        return Results.success(ticketSkuService.getTicketSkuById(id));
    }

    @PutMapping("/update")
    public Result<Void> updateTicketSku(@RequestBody TicketSkuDO requestParam) {
        ticketSkuService.updateById(requestParam);
        return Results.success();
    }

    /**
     * 增发票种库存
     *
     * @param skuId 票种ID
     * @param count 增发数量
     */
    @NoDuplicateSubmit(message = "请勿短时间内重复增发库存")
    @PostMapping("/increase-stock")
    public Result<Void> increaseStock(@RequestParam Long skuId, @RequestParam Integer count) {
        ticketSkuService.increaseStock(skuId, count);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteTicketSku(@PathVariable("id") Long id) {
        ticketSkuService.deleteTicketSku(id);
        return Results.success();
    }
}
