package com.mongxin.livestart.pay.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.pay.dto.RefundCreateRequest;
import com.mongxin.livestart.pay.dto.RefundCreateResponse;
import com.mongxin.livestart.pay.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay/refund")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @PostMapping
    public Result<RefundCreateResponse> create(@Valid @RequestBody RefundCreateRequest request,
                                               @RequestHeader("userId") Long userId) {
        return Results.success(refundService.create(request, userId));
    }
}
