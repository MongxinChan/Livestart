package com.mongxin.livestart.distribution.controller;

import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.dto.resp.SaleStagePreviewRespDTO;
import com.mongxin.livestart.distribution.dto.resp.SaleStageRespDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.framework.exception.ClientException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 演唱会发布 Controller
 */
@Tag(name = "演出票务分销秒杀 - 演唱会发布管理")
@RestController
@RequestMapping("/api/live-start/distribution/v1/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Value("${livestart.distribution.internal-token:change-me}")
    private String internalToken;

    @Operation(summary = "发布演唱会及票档", description = "主办方/平台发布演出信息并批量添加门票票档规格和发售库存")
    @PostMapping("/publish")
    public Result<Void> publishEvent(@Valid @RequestBody EventPublishReqDTO requestParam,
                                     @RequestHeader(value = "X-Livestart-Internal-Token", required = false) String requestToken) {
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(requestToken)) {
            throw new ClientException("无权发布分销演出");
        }
        eventService.publishEvent(requestParam);
        return Results.success();
    }

    @Operation(summary = "查询演出下一待开售阶段")
    @GetMapping("/{eventId}/next-sale-stage")
    public Result<SaleStagePreviewRespDTO> getNextSaleStage(@PathVariable Long eventId) {
        return Results.success(eventService.getNextSaleStage(eventId));
    }

    @Operation(summary = "查询演出全部开售阶段")
    @GetMapping("/{eventId}/sale-stages")
    public Result<List<SaleStageRespDTO>> listSaleStages(@PathVariable Long eventId) {
        return Results.success(eventService.listSaleStages(eventId));
    }
}
