package com.mongxin.livestart.admin.service;

import com.mongxin.livestart.admin.dao.entity.UserDO;
import com.mongxin.livestart.admin.dto.req.UserLoginReqDTO;
import com.mongxin.livestart.admin.dto.req.UserRegisterReqDTO;
import com.mongxin.livestart.admin.dto.req.UserUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.UserLoginRespDTO;
import com.mongxin.livestart.admin.dto.resp.UserRespDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 用户接口层
 *
 * @author Mongxin
 */
public interface UserService extends IService<UserDO> {

    /**
     * 根据手机号查询用户信息
     *
     * @param phone 手机号
     * @return 用户返回实体
     */
    UserRespDTO getUserByPhone(String phone);

    /**
     * 查询手机号是否存在
     *
     * @param phone 手机号
     * @return 存在返回 True,反之 False
     */
    Boolean availablePhone(String phone);

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
    Boolean checkLogin(String token, String phone);

    void logout(String phone, String token);

    /**
     * 上传用户头像（阿里云 OSS）
     * * @param inputStream      文件输入流
     * @param originalFilename 原始文件名
     * @return 文件的网络绝对访问路径 URL
     */
    String uploadAvatar(InputStream inputStream, String originalFilename) throws Exception;

    /**
     * 上传用户头像（本地 MinIO）
     * @return 文件的网络绝对访问路径 URL
     */
    String uploadAvatarByMinio(MultipartFile file) throws Exception;

    /**
     * 后台管理端分页查询用户列表
     *
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页结果
     */
    IPage<UserRespDTO> pageUser(int current, int size);
}
