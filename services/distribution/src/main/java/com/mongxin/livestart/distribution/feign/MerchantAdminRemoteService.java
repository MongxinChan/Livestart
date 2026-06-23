package com.mongxin.livestart.distribution.feign;

import com.mongxin.livestart.distribution.feign.dto.MerchantVenueRespDTO;
import com.mongxin.livestart.framework.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "livestart-merchant-admin",
        url = "${feign.merchant-admin.url:http://localhost:8003}",
        path = "/api/merchant-admin"
)
public interface MerchantAdminRemoteService {

    @GetMapping("/venue/{id}")
    Result<MerchantVenueRespDTO> getVenue(@PathVariable("id") Long id);
}
