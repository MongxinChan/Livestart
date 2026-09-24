package com.mongxin.livestart.distribution.feign;

import com.mongxin.livestart.distribution.feign.dto.MerchantVenueRespDTO;
import com.mongxin.livestart.framework.result.Result;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "livestart-merchant-admin",
        url = "${feign.merchant-admin.url:http://localhost:8003}",
        path = "/api/merchant-admin",
        configuration = MerchantAdminRemoteService.InternalTokenConfiguration.class
)
public interface MerchantAdminRemoteService {

    String INTERNAL_TOKEN_HEADER = "X-Livestart-Internal-Token";

    @GetMapping("/venue/{id}")
    Result<MerchantVenueRespDTO> getVenue(@PathVariable("id") Long id);

    class InternalTokenConfiguration {

        @Bean
        RequestInterceptor merchantAdminInternalTokenInterceptor(
                @Value("${livestart.distribution.internal-token:change-me}") String internalToken) {
            return template -> template.header(INTERNAL_TOKEN_HEADER, internalToken);
        }
    }
}
