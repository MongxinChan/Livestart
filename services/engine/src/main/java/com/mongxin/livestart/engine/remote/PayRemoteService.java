package com.mongxin.livestart.engine.remote;

import com.mongxin.livestart.engine.remote.dto.PayCreateRequestDTO;
import com.mongxin.livestart.engine.remote.dto.PayCreateResponseDTO;
import com.mongxin.livestart.engine.remote.dto.RefundCreateRequestDTO;
import com.mongxin.livestart.engine.remote.dto.RefundCreateResponseDTO;
import com.mongxin.livestart.framework.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "livestart-pay-service",
        url = "${feign.pay.url:http://127.0.0.1:8009}"
)
public interface PayRemoteService {

    @PostMapping("/api/pay/create")
    Result<PayCreateResponseDTO> create(@RequestBody PayCreateRequestDTO request,
                                        @RequestHeader("userId") String userId);

    @PostMapping("/api/pay/refund")
    Result<RefundCreateResponseDTO> refund(@RequestBody RefundCreateRequestDTO request,
                                           @RequestHeader("userId") String userId,
                                           @RequestHeader("X-Internal-Token") String internalToken);
}
