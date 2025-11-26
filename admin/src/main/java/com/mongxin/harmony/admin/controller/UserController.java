package com.mongxin.harmony.admin.controller;


import cn.hutool.core.bean.BeanUtil;
import com.mongxin.harmony.admin.common.convention.result.Result;
import com.mongxin.harmony.admin.common.convention.result.Results;
import com.mongxin.harmony.admin.dto.req.UserLoginReqDTO;
import com.mongxin.harmony.admin.dto.req.UserRegisterReqDTO;
import com.mongxin.harmony.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.harmony.admin.dto.resp.UserActualRespDTO;
import com.mongxin.harmony.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.harmony.admin.dto.resp.UserRespDTO;
import com.mongxin.harmony.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author Mongxin
 */
@RestController
@RequiredArgsConstructor //通过构造器方式注入
@Slf4j
public class UserController {

//  @Autowired

    private final UserService userService;


    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/saas-short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }

    /**
     * 根据用户名查询无脱敏用户信息
     */
    @GetMapping("/api/saas-short-link/admin/v1/actual/{username}")
    public Result<UserActualRespDTO> getActualUserByUserName(
            @PathVariable("username") String username) {
        return Results.success(
                BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/api/saas-short-link/admin/v1/has-username/{username}")
    public Result<Boolean> availableUserName(@PathVariable("username") String username) {
        return Results.success(userService.availableUserName(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/saas-short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/api/saas-short-link/admin/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/saas-short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        /* log.error("【Controller】接收 username={}", requestParam.getUsername()); */
        return Results.success(userService.login(requestParam));
    }


    /**
     * 检查用户是否登录
     */
    @GetMapping("/api/saas-short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username,
                                      @RequestParam("token") String token) {
        return Results.success(userService.checkLogin(username, token));
    }

    /**
     * 用户退出登录
     *
     * @param username
     * @return
     */
    @DeleteMapping("/api/saas-short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username,
                               @RequestParam("token") String token) {
        userService.logout(username, token);
        return Results.success();
    }
}
