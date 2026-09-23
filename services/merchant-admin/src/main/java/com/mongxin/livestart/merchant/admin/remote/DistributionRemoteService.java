package com.mongxin.livestart.merchant.admin.remote;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.merchant.admin.remote.dto.DistributionEventPublishReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "livestart-distribution",
        url = "${feign.distribution.url:http://localhost:8005}",
        path = "/api/live-start/distribution/v1"
)
public interface DistributionRemoteService {

    String INTERNAL_TOKEN_HEADER = "X-Livestart-Internal-Token";

    @PostMapping("/event/publish")
    Result<Void> publishEvent(@RequestBody DistributionEventPublishReqDTO requestParam,
                              @RequestHeader(INTERNAL_TOKEN_HEADER) String internalToken);
}
