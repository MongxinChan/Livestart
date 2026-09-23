package com.mongxin.livestart.admin.controller;

import com.mongxin.livestart.admin.common.convention.result.Result;
import com.mongxin.livestart.admin.common.convention.result.Results;
import com.mongxin.livestart.admin.dto.req.UserCodeLoginReqDTO;
import com.mongxin.livestart.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.livestart.admin.dto.req.UserVenueAdminBindReqDTO;
import com.mongxin.livestart.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.livestart.admin.dto.resp.UserRespDTO;
import com.mongxin.livestart.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Mongxin
 */
@RestController
@RequiredArgsConstructor // 通过构造器方式注入
@Slf4j
public class UserController {

    // @Autowired

    private final UserService userService;

    /**
     * 修改用户
     */
    @PutMapping("/api/live-start/admin/v1/user")
    public Result<Void> update(@RequestBody @Validated UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 检查用户是否登录
     */
    @GetMapping("/api/live-start/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestHeader("token") String token) {
        return Results.success(userService.checkLogin(token));
    }

    /**
     * 获取当前登录用户的完整画像（含 userType，用于前端按角色过滤菜单/路由）
     * <p>
     * 必须在登录后调用，依赖网关或本地 UserTransmitFilter 注入的 phone 头。
     */
    @GetMapping("/api/live-start/admin/v1/user/me")
    public Result<UserRespDTO> getMyself() {
        String phone = com.mongxin.livestart.admin.common.biz.user.UserContext.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new com.mongxin.livestart.admin.common.convention.exception.ClientException("当前用户未登录");
        }
        return Results.success(userService.getUserByPhone(phone));
    }

    /**
     * 用户退出登录
     */
    @DeleteMapping("/api/live-start/admin/v1/user/logout")
    public Result<Void> logout(@RequestHeader("token") String token) {
        userService.logout(token);
        return Results.success();
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/api/live-start/admin/v1/user/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        return Results.success(userService.uploadAvatar(file.getInputStream(), file.getOriginalFilename()));
    }

    /**
     * 上传用户头像(Minio)
     */
    @PostMapping("/api/live-start/admin/v1/user/MinioAvatar")
    public Result<String> uploadAvatarByMinio(@RequestParam("file") MultipartFile file) throws Exception {
        return Results.success(userService.uploadAvatarByMinio(file));
    }

    /**
     * 后台管理端分页查询用户列表
     */
    @GetMapping("/api/live-start/admin/v1/user/page")
    public Result<IPage<UserRespDTO>> pageUser(
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortOrder", required = false) String sortOrder,
            @RequestParam(value = "userType", required = false) Integer userType,
            @RequestParam(value = "phone", required = false) String phone) {
        return Results.success(userService.pageUser(current, size, sortField, sortOrder, userType, phone));
    }

    /**
     * 发送登录/注册手机验证码
     */
    @PostMapping("/api/live-start/admin/v1/user/send-code")
    public Result<Void> sendCode(@RequestParam("phone") String phone, HttpServletRequest request) {
        userService.sendCode(phone, resolveClientIp(request));
        return Results.success();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String gatewayClientIp = request.getHeader("X-Livestart-Client-IP");
        if (gatewayClientIp != null && !gatewayClientIp.isBlank()) {
            return gatewayClientIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 验证码快捷登录与自动注册
     */
    @PostMapping("/api/live-start/admin/v1/user/login/code")
    public Result<UserLoginRespDTO> loginByCode(@RequestBody @Validated UserCodeLoginReqDTO requestParam) {
        return Results.success(userService.loginByCode(requestParam.getPhone(), requestParam.getCode()));
    }

    @PutMapping("/api/live-start/admin/v1/user/type")
    public Result<Void> updateUserType(
            @RequestParam("userId") Long userId,
            @RequestParam("userType") Integer userType) {
        userService.updateUserType(userId, userType);
        return Results.success();
    }

    @PutMapping("/api/live-start/admin/v1/user/status")
    public Result<Void> updateUserStatus(
            @RequestParam("userId") Long userId,
            @RequestParam("status") Integer status) {
        userService.updateUserStatus(userId, status);
        return Results.success();
    }

    @PutMapping("/api/live-start/admin/v1/user/venue-admin")
    public Result<Void> bindVenueAdmin(@RequestBody @Validated UserVenueAdminBindReqDTO requestParam) {
        userService.bindVenueAdmin(requestParam.getUserId(), requestParam.getVenueId());
        return Results.success();
    }

    @GetMapping("/api/live-start/admin/v1/user/simple/list")
    public Result<List<UserRespDTO>> listSimpleUsersByIds(
            @RequestParam("userIds") List<Long> userIds,
            @RequestHeader("X-Livestart-Internal-Token") String internalToken) {
        return Results.success(userService.listSimpleUsersByIds(userIds, internalToken));
    }
}
