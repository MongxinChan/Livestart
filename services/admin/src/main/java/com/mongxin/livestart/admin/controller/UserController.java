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
        // 由于现已统一为 UserRespDTO，若要实现真正脱敏，后续请结合 AOP 或新的字段来隔离
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
     * 注册用户
     */
    @PostMapping("/api/live-start/admin/v1/user")
    public Result<Void> register(@RequestBody @Validated UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
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
     * 用户退出登录
     *
     * @param phone
     * @return
     */
    @DeleteMapping("/api/live-start/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("phone") String phone,
                               @RequestParam("token") String token) {
        userService.logout(phone, token);
        return Results.success();
    }
}
