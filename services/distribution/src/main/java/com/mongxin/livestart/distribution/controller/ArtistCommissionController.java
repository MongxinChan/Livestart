package com.mongxin.livestart.distribution.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.distribution.dto.resp.ArtistCommissionRespDTO;
import com.mongxin.livestart.distribution.dto.resp.InviteCodeRespDTO;
import com.mongxin.livestart.distribution.service.ArtistCommissionService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 艺人推广宣发码与个税代扣分成管理 Controller
 */
@Tag(name = "演出票务分销秒杀 - 艺人推广个税与分成收益管理")
@RestController
@RequestMapping("/api/live-start/distribution/v1/artist")
@RequiredArgsConstructor
public class ArtistCommissionController {

    private final ArtistCommissionService artistCommissionService;

    @Operation(summary = "获取或生成艺人推广宣发码", description = "获取或自动双检生成当前登录艺人的宣发专属码、绑定的歌迷数及实得收益数据")
    @GetMapping("/code")
    public Result<InviteCodeRespDTO> getOrCreateArtistPromoCode() {
        return Results.success(artistCommissionService.getOrCreateArtistPromoCode());
    }

    @Operation(summary = "分页检索本艺人的提成与个税明细", description = "分页拉取该艺人的票房推广所得，每一笔均体现代扣20%劳务个税明细及延迟结算状态")
    @Parameters({
            @Parameter(name = "pageNo", description = "页码", required = true, example = "1"),
            @Parameter(name = "pageSize", description = "每页大小", required = true, example = "10"),
            @Parameter(name = "status", description = "结算状态 0:待结算(在途) 1:已结算(到账) 2:已取消", required = false)
    })
    @GetMapping("/commission/page")
    public Result<IPage<ArtistCommissionRespDTO>> pageQueryArtistCommissions(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return Results.success(artistCommissionService.pageQueryArtistCommissions(pageNo, pageSize, status));
    }
}
