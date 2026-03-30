package com.mongxin.livestart.admin.service;

import com.mongxin.livestart.admin.dao.entity.UserVisitorDO;
import com.mongxin.livestart.admin.dto.req.VisitorAddReqDTO;
import com.mongxin.livestart.admin.dto.req.VisitorUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.VisitorRespDTO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 常用观演人接口层
 *
 * @author Mongxin
 */
public interface VisitorService extends IService<UserVisitorDO> {

    /**
     * 新增常用观演人
     * <p>
     * 校验证件格式 → 判重（同一用户下同一证件号） → AES 加密 card_no → SHA-256 生成 card_no_hash → 入库
     *
     * @param requestParam 新增观演人请求参数
     */
    void addVisitor(VisitorAddReqDTO requestParam);

    /**
     * 修改观演人信息（姓名、手机号）
     * <p>
     * 注意：证件号码不允许修改，如需更换请先删除再重新添加
     *
     * @param requestParam 修改观演人请求参数
     */
    void updateVisitor(VisitorUpdateReqDTO requestParam);

    /**
     * 逻辑删除观演人
     *
     * @param id 观演人 ID
     */
    void removeVisitor(Long id);

    /**
     * 查询当前登录用户的观演人列表
     *
     * @return 观演人列表
     */
    List<VisitorRespDTO> listVisitors();

    /**
     * 查询单个观演人详情
     *
     * @param id 观演人 ID
     * @return 观演人详情
     */
    VisitorRespDTO getVisitorById(Long id);
}
