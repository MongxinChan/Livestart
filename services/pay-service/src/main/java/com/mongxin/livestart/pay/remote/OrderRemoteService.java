package com.mongxin.livestart.pay.remote;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.pay.remote.dto.PayableOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "livestart-engine", url = "${feign.engine.url:http://127.0.0.1:8004}")
public interface OrderRemoteService {

    @GetMapping("/api/engine/internal/orders/{orderNo}/payable")
    Result<PayableOrderDTO> getPayableOrder(@PathVariable("orderNo") String orderNo,
                                            @RequestParam("userId") Long userId,
                                            @RequestHeader("X-Livestart-Internal-Token") String internalToken);
}
