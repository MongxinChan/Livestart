package com.mongxin.livestart.admin.controller;

import com.mongxin.livestart.admin.common.convention.result.Result;
import com.mongxin.livestart.admin.common.convention.result.Results;
import com.mongxin.livestart.admin.dto.req.UserLoginReqDTO;
import com.mongxin.livestart.admin.dto.req.UserRegisterReqDTO;
import com.mongxin.livestart.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.livestart.admin.dto.resp.UserRespDTO;
import com.mongxin.livestart.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
     * 根据手机号查询用户信息
     */
    @GetMapping("/api/live-start/admin/v1/user/{phone}")
    public Result<UserRespDTO> getUserByPhone(@PathVariable("phone") String phone) {
        return Results.success(userService.getUserByPhone(phone));
    }

    /**
     * 根据手机号查询无脱敏用户信息
     */
    @GetMapping("/api/live-start/admin/v1/actual/{phone}")
    public Result<UserRespDTO> getActualUserByPhone(
            @PathVariable("phone") String phone) {
        // TODO:由于现已统一为 UserRespDTO，若要实现真正脱敏，后续请结合 AOP 或新的字段来隔离
        return Results.success(userService.getUserByPhone(phone));
    }

    /**
     * 查询手机号是否存在
     */
    @GetMapping("/api/live-start/admin/v1/has-phone/{phone}")
    public Result<Boolean> availablePhone(@PathVariable("phone") String phone) {
        return Results.success(userService.availablePhone(phone));
    }

    /**
     * 注册用户（注册成功直接返回 token，前端拿到即视为已登录）
     */
    @PostMapping("/api/live-start/admin/v1/user")
    public Result<UserLoginRespDTO> register(@RequestBody @Validated UserRegisterReqDTO requestParam) {
        return Results.success(userService.register(requestParam));
    }

    /**
     * 修改用户
     */
    @PutMapping("/api/live-start/admin/v1/user")
    public Result<Void> update(@RequestBody @Validated UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/live-start/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody @Validated UserLoginReqDTO requestParam) {
        /* log.error("【Controller】接收 username={}", requestParam.getUsername()); */
        return Results.success(userService.login(requestParam));
    }

    /**
     * 检查用户是否登录
     */
    @GetMapping("/api/live-start/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("phone") String phone,
            @RequestParam("token") String token) {
        return Results.success(userService.checkLogin(phone, token));
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
    public Result<Void> logout(@RequestParam("phone") String phone,
            @RequestParam("token") String token) {
        userService.logout(phone, token);
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
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return Results.success(userService.pageUser(current, size));
    }

    /**
     * 发送登录/注册手机验证码
     */
    @PostMapping("/api/live-start/admin/v1/user/send-code")
    public Result<Void> sendCode(@RequestParam("phone") String phone) {
        userService.sendCode(phone);
        return Results.success();
    }

    /**
     * 验证码快捷登录与自动注册
     */
    @PostMapping("/api/live-start/admin/v1/user/login/code")
    public Result<UserLoginRespDTO> loginByCode(
            @RequestParam("phone") String phone,
            @RequestParam("code") String code) {
        return Results.success(userService.loginByCode(phone, code));
    }

    /**
     * 临时管理接口：提升用户为管理员（仅用于开发测试）
     * TODO: 生产环境应删除此接口或增加严格的权限校验
     */
    @PutMapping("/api/live-start/admin/v1/user/promote-admin")
    public Result<Void> promoteToAdmin(
            @RequestParam("phone") String phone,
            @RequestParam("userType") Integer userType) {
        userService.updateUserType(phone, userType);
        return Results.success();
    }

    @GetMapping("/api/live-start/admin/v1/user/simple/list")
    public Result<List<UserRespDTO>> listSimpleUsersByIds(@RequestParam("userIds") List<Long> userIds) {
        return Results.success(userService.listSimpleUsersByIds(userIds));
    }
}
