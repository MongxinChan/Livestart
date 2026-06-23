package com.mongxin.livestart.engine.remote;

import com.mongxin.livestart.engine.remote.dto.AdminUserSimpleRespDTO;
import com.mongxin.livestart.framework.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "livestart-admin",
        url = "${feign.admin.url:http://localhost:8001}"
)
public interface AdminRemoteService {

    @GetMapping("/api/live-start/admin/v1/user/simple/list")
    Result<List<AdminUserSimpleRespDTO>> listSimpleUsersByIds(@RequestParam("userIds") List<Long> userIds);
}
