package com.mongxin.livestart.engine.remote;

import com.mongxin.livestart.engine.remote.dto.DistributionSaleStageRespDTO;
import com.mongxin.livestart.framework.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 引擎端对分销系统 (livestart-distribution) 的远程调用 Feign 客户端
 */
@FeignClient(
        name = "livestart-distribution",
        url = "${feign.distribution.url:http://localhost:8005}",
        path = "/api/live-start/distribution/v1"
)
public interface DistributionRemoteService {

    /**
     * 查询演出所包含的所有售票阶段配置信息
     *
     * @param eventId 关联的演出项目主键ID
     * @return 返回演出售票阶段的详细配置信息列表
     */
    @GetMapping("/event/{eventId}/sale-stages")
    Result<List<DistributionSaleStageRespDTO>> listSaleStages(@PathVariable("eventId") Long eventId);
}
