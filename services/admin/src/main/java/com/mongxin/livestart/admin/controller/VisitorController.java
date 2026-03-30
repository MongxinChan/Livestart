package com.mongxin.livestart.admin.controller;

import com.mongxin.livestart.admin.common.convention.result.Result;
import com.mongxin.livestart.admin.common.convention.result.Results;
import com.mongxin.livestart.admin.dto.req.VisitorAddReqDTO;
import com.mongxin.livestart.admin.dto.req.VisitorUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.VisitorRespDTO;
import com.mongxin.livestart.admin.service.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用观演人控制器
 * <p>
 * 所有接口均需通过网关鉴权（Header 中携带有效的 userId），
 * 服务端从 UserContext 中获取当前登录用户，不信任客户端传入的 userId。
 *
 * @author Mongxin
 */
@RestController
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    /**
     * 新增常用观演人
     */
    @PostMapping("/api/live-start/admin/v1/visitor")
    public Result<Void> addVisitor(@RequestBody @Validated VisitorAddReqDTO requestParam) {
        visitorService.addVisitor(requestParam);
        return Results.success();
    }

    /**
     * 修改观演人信息（姓名、手机号）
     * 注意：证件号不可修改，如需更换请先删除再重新添加
     */
    @PutMapping("/api/live-start/admin/v1/visitor")
    public Result<Void> updateVisitor(@RequestBody @Validated VisitorUpdateReqDTO requestParam) {
        visitorService.updateVisitor(requestParam);
        return Results.success();
    }

    /**
     * 删除观演人（逻辑删除）
     */
    @DeleteMapping("/api/live-start/admin/v1/visitor/{id}")
    public Result<Void> removeVisitor(@PathVariable("id") Long id) {
        visitorService.removeVisitor(id);
        return Results.success();
    }

    /**
     * 查询当前登录用户的观演人列表
     */
    @GetMapping("/api/live-start/admin/v1/visitor/list")
    public Result<List<VisitorRespDTO>> listVisitors() {
        return Results.success(visitorService.listVisitors());
    }

    /**
     * 查询单个观演人详情
     */
    @GetMapping("/api/live-start/admin/v1/visitor/{id}")
    public Result<VisitorRespDTO> getVisitorById(@PathVariable("id") Long id) {
        return Results.success(visitorService.getVisitorById(id));
    }
}
