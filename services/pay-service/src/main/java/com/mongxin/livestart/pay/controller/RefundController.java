package com.mongxin.livestart.pay.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.pay.dto.RefundCreateRequest;
import com.mongxin.livestart.pay.dto.RefundCreateResponse;
import com.mongxin.livestart.pay.service.RefundService;
import com.mongxin.livestart.pay.config.PayServiceProperties;
import com.mongxin.livestart.framework.exception.ClientException;
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
    private final PayServiceProperties payServiceProperties;

    @PostMapping
    public Result<RefundCreateResponse> create(@Valid @RequestBody RefundCreateRequest request,
                                               @RequestHeader("userId") Long userId,
                                               @RequestHeader("X-Internal-Token") String internalToken) {
        if (!payServiceProperties.getInternalToken().equals(internalToken)) {
            throw new ClientException("内部调用认证失败");
        }
        return Results.success(refundService.create(request, userId));
    }
}
