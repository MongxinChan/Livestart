package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.EventStyleRelationDO;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerStyleRelationDO;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventStyleRelationMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerStyleRelationMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.dto.req.StylePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.StyleSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StylePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StyleQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.StyleService;
import com.mongxin.livestart.merchant.admin.service.security.MerchantAccessControl;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 音乐风格服务实现层
 */
@Service
@RequiredArgsConstructor
public class StyleServiceImpl extends ServiceImpl<StyleMapper, StyleDO> implements StyleService {

    private final PerformerMapper performerMapper;
    private final PerformerStyleRelationMapper performerStyleRelationMapper;
    private final EventStyleRelationMapper eventStyleRelationMapper;
    private final MerchantAccessControl accessControl;

    @Override
    @LogRecord(success = "创建音乐风格：{{#requestParam.name}}", fail = "创建音乐风格失败：{{#requestParam.name}}",
            type = "Style", subType = "Create",
            bizNo = "{{#bizNo}}", extra = "{{#modifiedData}}")
    public void createStyle(StyleSaveReqDTO requestParam) {
        accessControl.requireSuperAdminAccess();
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .eq(StyleDO::getCode, requestParam.getCode());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("风格代码 [" + requestParam.getCode() + "] 已存在，请勿重复录入");
        }
        StyleDO styleDO = BeanUtil.toBean(requestParam, StyleDO.class);
        save(styleDO);
        LogRecordContext.putVariable("bizNo", styleDO.getId());
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(styleDO));
    }

    @Override
    public IPage<StylePageQueryRespDTO> pageQueryStyles(StylePageQueryReqDTO requestParam) {
        accessControl.requireAdminAccess();
        LambdaQueryWrapper<StyleDO> queryWrapper = Wrappers.lambdaQuery(StyleDO.class)
                .like(StrUtil.isNotBlank(requestParam.getName()), StyleDO::getName, requestParam.getName())
                .orderByDesc(StyleDO::getId);
        IPage<StyleDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, StylePageQueryRespDTO.class));
    }

    @Override
    public StyleQueryRespDTO getStyleById(Long id) {
        accessControl.requireAdminAccess();
        StyleDO styleDO = getById(id);
        return BeanUtil.toBean(styleDO, StyleQueryRespDTO.class);
    }

    @Override
    @LogRecord(success = "修改音乐风格：风格ID {{#requestParam.id}}", fail = "修改音乐风格失败：风格ID {{#requestParam.id}}",
            type = "Style", subType = "Update",
            bizNo = "{{#requestParam.id}}", extra = "{{#modifiedData}}")
    public void updateStyle(StyleSaveReqDTO requestParam) {
        accessControl.requireSuperAdminAccess();
        StyleDO originalStyle = getById(requestParam.getId());
        if (originalStyle == null) {
            throw new ServiceException("音乐风格不存在");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(originalStyle));
        StyleDO styleDO = BeanUtil.toBean(requestParam, StyleDO.class);
        updateById(styleDO);
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(getById(requestParam.getId())));
    }

    @Override
    @LogRecord(success = "删除音乐风格：风格ID {{#id}}", fail = "删除音乐风格失败：风格ID {{#id}}",
            type = "Style", subType = "Delete", bizNo = "{{#id}}")
    @Transactional(rollbackFor = Exception.class)
    public void deleteStyle(Long id) {
        accessControl.requireSuperAdminAccess();
        StyleDO originalStyle = getById(id);
        if (originalStyle == null) {
            throw new ServiceException("音乐风格不存在");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(originalStyle));
        long performerCount = performerMapper.selectCount(Wrappers.lambdaQuery(PerformerDO.class)
                .eq(PerformerDO::getStyleId, id));
        long performerRelationCount = performerStyleRelationMapper.selectCount(
                Wrappers.lambdaQuery(PerformerStyleRelationDO.class)
                        .eq(PerformerStyleRelationDO::getStyleId, id));
        long eventRelationCount = eventStyleRelationMapper.selectCount(
                Wrappers.lambdaQuery(EventStyleRelationDO.class)
                        .eq(EventStyleRelationDO::getStyleId, id));
        if (performerCount + performerRelationCount + eventRelationCount > 0) {
            throw new ServiceException("音乐风格仍被艺人或演出使用，不能删除");
        }
        removeById(id);
    }
}
