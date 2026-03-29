package com.mongxin.livestart.admin.dao.entity;

import lombok.Data;

/**
 * 用户信息视图对象
 * 聚合了 UserDO 和 UserProfileDO 的字段，统一提供给前端
 */
@Data
public class UserVO {

    /**
     * 账号信息
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 具有唯一性
     */
    private String phone;

    /**
     * 是否已认证
     */
    private Integer isVerified;

    /**
     * 根据userType显示不同的页面，也就是艺人页，场馆页
     */
    private Integer userType;

    /**
     * 社交信息
     */
    private String mail;

    /**
     * 社交头像，默认傻冰的头像
     */
    private String avatar;

    /**
     * 个性签名，默认都是“live与love一样重要”
     */
    private String signature;

    /**
     * 性别，如果后续有添加推荐系统可以根据此推荐
     */
    private Integer gender;
}