package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 艺人/乐队服务实现层
 */
@Service
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {

    @Autowired
    private StyleMapper styleMapper;

    @Override
    public void createPerformer(PerformerSaveReqDTO requestParam) {
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .eq(PerformerDO::getName, requestParam.getName());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("艺人/乐队名称已存在，请勿重复录入");
        }
        PerformerDO performerDO = new PerformerDO();
        performerDO.setName(requestParam.getName());
        performerDO.setStatus(requestParam.getStatus());
        
        // 映射字段：兼容前端的 avatarUrl 和 description 属性
        String finalAvatar = StrUtil.isNotBlank(requestParam.getAvatarUrl()) ? requestParam.getAvatarUrl() : requestParam.getAvatar();
        String finalBio = StrUtil.isNotBlank(requestParam.getDescription()) ? requestParam.getDescription() : requestParam.getBio();
        performerDO.setAvatar(finalAvatar);
        performerDO.setBio(finalBio);

        // 风格关联处理（自动隐式匹配与查表注册）
        if (StrUtil.isNotBlank(requestParam.getGenre())) {
            performerDO.setStyleId(getOrCreateStyleId(requestParam.getGenre()));
        } else if (requestParam.getStyleId() != null) {
            performerDO.setStyleId(requestParam.getStyleId());
        }

        save(performerDO);
    }

    @Override
    public IPage<PerformerPageQueryRespDTO> pageQueryPerformers(PerformerPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class)
                .like(StrUtil.isNotBlank(requestParam.getName()), PerformerDO::getName, requestParam.getName())
                .orderByDesc(PerformerDO::getId);
        IPage<PerformerDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> {
            PerformerPageQueryRespDTO dto = new PerformerPageQueryRespDTO();
            dto.setId(each.getId());
            dto.setName(each.getName());
            dto.setStyleId(each.getStyleId());
            dto.setStatus(each.getStatus());
            dto.setAvatar(each.getAvatar());
            dto.setBio(each.getBio());

            // 映射前端渲染字段
            dto.setAvatarUrl(each.getAvatar());
            dto.setDescription(each.getBio());

            // 查表动态装配风格名称
            if (each.getStyleId() != null) {
                StyleDO style = styleMapper.selectById(each.getStyleId());
                if (style != null) {
                    dto.setGenre(style.getName());
                }
            }
            return dto;
        });
    }

    @Override
    public PerformerQueryRespDTO getPerformerById(Long id) {
        PerformerDO performerDO = getById(id);
        if (performerDO == null) {
            return null;
        }
        PerformerQueryRespDTO dto = BeanUtil.toBean(performerDO, PerformerQueryRespDTO.class);
        
        // 映射前端渲染字段
        dto.setAvatarUrl(performerDO.getAvatar());
        dto.setDescription(performerDO.getBio());

        // 查表装配风格名称
        if (performerDO.getStyleId() != null) {
            StyleDO style = styleMapper.selectById(performerDO.getStyleId());
            if (style != null) {
                dto.setGenre(style.getName());
            }
        }
        return dto;
    }

    @Override
    public void updatePerformer(PerformerSaveReqDTO requestParam) {
        PerformerDO performerDO = getById(requestParam.getId());
        if (performerDO == null) {
            throw new ServiceException("未找到对应的艺人数据");
        }
        performerDO.setName(requestParam.getName());
        performerDO.setStatus(requestParam.getStatus());
        
        // 映射字段：兼容前端的 avatarUrl 和 description 属性
        String finalAvatar = StrUtil.isNotBlank(requestParam.getAvatarUrl()) ? requestParam.getAvatarUrl() : requestParam.getAvatar();
        String finalBio = StrUtil.isNotBlank(requestParam.getDescription()) ? requestParam.getDescription() : requestParam.getBio();
        performerDO.setAvatar(finalAvatar);
        performerDO.setBio(finalBio);

        // 风格关联处理（自动隐式匹配与查表注册）
        if (StrUtil.isNotBlank(requestParam.getGenre())) {
            performerDO.setStyleId(getOrCreateStyleId(requestParam.getGenre()));
        } else {
            performerDO.setStyleId(requestParam.getStyleId());
        }

        updateById(performerDO);
    }

    @Override
    public void deletePerformer(Long id) {
        removeById(id);
    }

    /**
     * 隐式匹配或创建风格，返回风格ID
     */
    private Long getOrCreateStyleId(String genreName) {
        LambdaQueryWrapper<StyleDO> query = Wrappers.lambdaQuery(StyleDO.class)
                .eq(StyleDO::getName, genreName);
        StyleDO style = styleMapper.selectOne(query);
        if (style != null) {
            return style.getId();
        }
        // 隐式自动建档风格
        StyleDO newStyle = new StyleDO();
        newStyle.setName(genreName);
        newStyle.setCode(genreName.toUpperCase());
        newStyle.setDescription(genreName + " 风格分类");
        styleMapper.insert(newStyle);
        return newStyle.getId();
    }
}
