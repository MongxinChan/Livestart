package com.mongxin.livestart.engine.remote;

import com.mongxin.livestart.engine.remote.dto.MerchantEventRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuRespDTO;
import com.mongxin.livestart.framework.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 商户后台管理服务 Feign Client
 * <p>
 * 生产环境应使用 Nacos 服务发现（name = "livestart-merchant-admin"），
 * 开发阶段暂用 url 直连。
 */
@FeignClient(
        name = "livestart-merchant-admin",
        url = "${feign.merchant-admin.url:http://localhost:8003}",
        path = "/api/merchant-admin"
)
public interface MerchantAdminRemoteService {

    /**
     * 分页查询演出列表
     */
    @GetMapping("/event/page")
    Result<IPage<MerchantEventRespDTO>> pageQueryEvents(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "50") Integer size
    );

    /**
     * 查询演出详情
     */
    @GetMapping("/event/{id}")
    Result<MerchantEventRespDTO> getEvent(@PathVariable("id") Long id);

    /**
     * 分页查询票档列表
     */
    @GetMapping("/ticket-sku/page")
    Result<IPage<MerchantTicketSkuRespDTO>> pageQueryTicketSkus(
            @RequestParam(value = "eventId", required = false) Long eventId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "100") Integer size
    );
}
