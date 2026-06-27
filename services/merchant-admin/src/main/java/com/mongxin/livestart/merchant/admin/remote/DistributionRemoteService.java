package com.mongxin.livestart.merchant.admin.remote;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.merchant.admin.remote.dto.DistributionEventPublishReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "livestart-distribution",
        url = "${feign.distribution.url:http://localhost:8005}",
        path = "/api/live-start/distribution/v1"
)
public interface DistributionRemoteService {

    @PostMapping("/event/publish")
    Result<Void> publishEvent(@RequestBody DistributionEventPublishReqDTO requestParam);
}
