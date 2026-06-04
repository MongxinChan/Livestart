package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerDO;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.entity.PerformerStyleRelationDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.PerformerStyleRelationMapper;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 艺人/乐队服务实现层
 */
@Service
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {

    @Autowired
    private StyleMapper styleMapper;

    @Autowired
    private PerformerStyleRelationMapper performerStyleRelationMapper;

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

        // 风格关联处理（优先采用前端传过来的 styleIds 列表，如果没有则尝试原来的单风格）
        if (CollUtil.isNotEmpty(requestParam.getStyleIds())) {
            performerDO.setStyleId(requestParam.getStyleIds().get(0));
        } else if (StrUtil.isNotBlank(requestParam.getGenre())) {
            performerDO.setStyleId(getOrCreateStyleId(requestParam.getGenre()));
        } else if (requestParam.getStyleId() != null) {
            performerDO.setStyleId(requestParam.getStyleId());
        }

        save(performerDO);

        // 持久化多对多关联中间表
        if (CollUtil.isNotEmpty(requestParam.getStyleIds())) {
            for (Long styleId : requestParam.getStyleIds()) {
                performerStyleRelationMapper.insert(PerformerStyleRelationDO.builder()
                        .performerId(performerDO.getId())
                        .styleId(styleId)
                        .build());
            }
        } else if (performerDO.getStyleId() != null) {
            performerStyleRelationMapper.insert(PerformerStyleRelationDO.builder()
                    .performerId(performerDO.getId())
                    .styleId(performerDO.getStyleId())
                    .build());
        }
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

            // 查中间表获取多风格ID
            List<PerformerStyleRelationDO> relations = performerStyleRelationMapper.selectList(
                    Wrappers.lambdaQuery(PerformerStyleRelationDO.class)
                            .eq(PerformerStyleRelationDO::getPerformerId, each.getId())
            );
            List<Long> styleIds = relations.stream().map(PerformerStyleRelationDO::getStyleId).collect(Collectors.toList());
            
            // 存量数据平滑自动迁移
            if (CollUtil.isEmpty(styleIds) && each.getStyleId() != null) {
                styleIds = new ArrayList<>();
                styleIds.add(each.getStyleId());
                performerStyleRelationMapper.insert(PerformerStyleRelationDO.builder()
                        .performerId(each.getId())
                        .styleId(each.getStyleId())
                        .build());
            }

            dto.setStyleIds(styleIds);

            // 动态拼接多风格名称
            if (CollUtil.isNotEmpty(styleIds)) {
                List<StyleDO> styles = styleMapper.selectBatchIds(styleIds);
                if (CollUtil.isNotEmpty(styles)) {
                    String genreNames = styles.stream().map(StyleDO::getName).collect(Collectors.joining(","));
                    dto.setGenre(genreNames);
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

        // 查中间表获取多风格ID
        List<PerformerStyleRelationDO> relations = performerStyleRelationMapper.selectList(
                Wrappers.lambdaQuery(PerformerStyleRelationDO.class)
                        .eq(PerformerStyleRelationDO::getPerformerId, performerDO.getId())
        );
        List<Long> styleIds = relations.stream().map(PerformerStyleRelationDO::getStyleId).collect(Collectors.toList());

        // 存量数据平滑自动迁移
        if (CollUtil.isEmpty(styleIds) && performerDO.getStyleId() != null) {
            styleIds = new ArrayList<>();
            styleIds.add(performerDO.getStyleId());
            performerStyleRelationMapper.insert(PerformerStyleRelationDO.builder()
                    .performerId(performerDO.getId())
                    .styleId(performerDO.getStyleId())
                    .build());
        }

        dto.setStyleIds(styleIds);

        // 动态拼接多风格名称
        if (CollUtil.isNotEmpty(styleIds)) {
            List<StyleDO> styles = styleMapper.selectBatchIds(styleIds);
            if (CollUtil.isNotEmpty(styles)) {
                String genreNames = styles.stream().map(StyleDO::getName).collect(Collectors.joining(","));
                dto.setGenre(genreNames);
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

        // 风格关联处理（优先采用多风格ID）
        if (CollUtil.isNotEmpty(requestParam.getStyleIds())) {
            performerDO.setStyleId(requestParam.getStyleIds().get(0));
        } else if (StrUtil.isNotBlank(requestParam.getGenre())) {
            performerDO.setStyleId(getOrCreateStyleId(requestParam.getGenre()));
        } else {
            performerDO.setStyleId(requestParam.getStyleId());
        }

        updateById(performerDO);

        // 更新多风格中间表（先删后增）
        performerStyleRelationMapper.delete(
                Wrappers.lambdaQuery(PerformerStyleRelationDO.class)
                        .eq(PerformerStyleRelationDO::getPerformerId, performerDO.getId())
        );

        if (CollUtil.isNotEmpty(requestParam.getStyleIds())) {
            for (Long styleId : requestParam.getStyleIds()) {
                performerStyleRelationMapper.insert(PerformerStyleRelationDO.builder()
                        .performerId(performerDO.getId())
                        .styleId(styleId)
                        .build());
            }
        } else if (performerDO.getStyleId() != null) {
            performerStyleRelationMapper.insert(PerformerStyleRelationDO.builder()
                    .performerId(performerDO.getId())
                    .styleId(performerDO.getStyleId())
                    .build());
        }
    }

    @Override
    public void deletePerformer(Long id) {
        removeById(id);
        // 级联清除多风格中间表记录
        performerStyleRelationMapper.delete(
                Wrappers.lambdaQuery(PerformerStyleRelationDO.class)
                        .eq(PerformerStyleRelationDO::getPerformerId, id)
        );
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
