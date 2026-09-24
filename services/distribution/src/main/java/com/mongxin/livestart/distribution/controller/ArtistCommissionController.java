package com.mongxin.livestart.distribution.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.distribution.dto.resp.ArtistCommissionRespDTO;
import com.mongxin.livestart.distribution.dto.resp.InviteCodeRespDTO;
import com.mongxin.livestart.distribution.dto.req.ArtistBindReqDTO;
import com.mongxin.livestart.distribution.dto.req.ArtistWithdrawalCreateReqDTO;
import com.mongxin.livestart.distribution.dto.resp.ArtistWalletRespDTO;
import com.mongxin.livestart.distribution.dto.resp.ArtistWithdrawalRespDTO;
import com.mongxin.livestart.distribution.service.ArtistCommissionService;
import com.mongxin.livestart.distribution.service.ArtistWalletService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 艺人推广宣发码与个税代扣分成管理 Controller
 */
@Tag(name = "演出票务分销秒杀 - 艺人推广个税与分成收益管理")
@RestController
@RequestMapping("/api/live-start/distribution/v1/artist")
@RequiredArgsConstructor
public class ArtistCommissionController {

    private final ArtistCommissionService artistCommissionService;
    private final ArtistWalletService artistWalletService;

    @Operation(summary = "歌迷绑定艺人推广关系")
    @PostMapping("/bind")
    public Result<Void> bindArtist(@Valid @RequestBody ArtistBindReqDTO request) {
        artistCommissionService.bindArtist(request);
        return Results.success();
    }

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

    @Operation(summary = "查询艺人钱包余额")
    @GetMapping("/wallet")
    public Result<ArtistWalletRespDTO> wallet() {
        return Results.success(artistWalletService.getCurrentWallet());
    }

    @Operation(summary = "提交艺人提现申请")
    @PostMapping("/withdrawals")
    public Result<ArtistWithdrawalRespDTO> createWithdrawal(
            @Valid @RequestBody ArtistWithdrawalCreateReqDTO request) {
        return Results.success(artistWalletService.createWithdrawal(request));
    }

    @Operation(summary = "查询艺人提现申请")
    @GetMapping("/withdrawals")
    public Result<IPage<ArtistWithdrawalRespDTO>> withdrawals(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Results.success(artistWalletService.pageWithdrawals(pageNo, pageSize));
    }

    @Operation(summary = "取消待审核提现申请")
    @PostMapping("/withdrawals/{withdrawalId}/cancel")
    public Result<Void> cancelWithdrawal(@PathVariable Long withdrawalId) {
        artistWalletService.cancelWithdrawal(withdrawalId);
        return Results.success();
    }

    @Operation(summary = "完成提现打款")
    @PostMapping("/withdrawals/{withdrawalId}/complete")
    public Result<Void> completeWithdrawal(@PathVariable Long withdrawalId,
                                           @RequestParam(required = false) String externalNo) {
        artistWalletService.completeWithdrawal(withdrawalId, externalNo);
        return Results.success();
    }
}
