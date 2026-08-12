package com.mongxin.livestart.pay.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.pay.dto.PayCreateRequest;
import com.mongxin.livestart.pay.dto.PayCreateResponse;
import com.mongxin.livestart.pay.service.PayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {
    private final PayService payService;

    @PostMapping("/create")
    public Result<PayCreateResponse> create(@Valid @RequestBody PayCreateRequest request,
                                            @RequestHeader("userId") Long userId) {
        return Results.success(payService.create(request, userId));
    }
}
