package com.mongxin.livestart.admin.service;

import com.mongxin.livestart.admin.dao.entity.UserDO;
import com.mongxin.livestart.admin.dto.req.UserLoginReqDTO;
import com.mongxin.livestart.admin.dto.req.UserRegisterReqDTO;
import com.mongxin.livestart.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.livestart.admin.dto.resp.UserRespDTO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户接口层
 *
 * @author Mongxin
 */
public interface UserService extends IService<UserDO> {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否存在
     *
     * @param username 用户名
     * @return 用户名存在返回 True,反之 False
     */
    Boolean availableUserName(String username);

    /**
     * 注册用户
     *
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 修改用户
     *
     * @param requestParam
     */
    void update(UserUpdateReqDTO requestParam);

    /**
     * 登录
     *
     * @param requestParam
     * @return
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * 检查登录
     *
     * @param token
     * @return
     */
    Boolean checkLogin(String token, String username);

    void logout(String username, String token);
}
