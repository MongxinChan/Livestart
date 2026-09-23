package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
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
import com.mongxin.livestart.merchant.admin.dto.req.PerformerImportExcelDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.PerformerSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.ImportResultRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.PerformerQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.PerformerService;
import com.mongxin.livestart.merchant.admin.service.security.MerchantAccessControl;
import com.mongxin.livestart.merchant.admin.toolkit.EasyExcelImportUtil;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 艺人/乐队服务实现层
 */
@Service
@RequiredArgsConstructor
public class PerformerServiceImpl extends ServiceImpl<PerformerMapper, PerformerDO> implements PerformerService {

    private final StyleMapper styleMapper;
    private final PerformerStyleRelationMapper performerStyleRelationMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MerchantAccessControl accessControl;
    private final ObjectProvider<PerformerService> selfProvider;

    @Override
    @LogRecord(success = "创建艺人/乐队：{{#requestParam.name}}", fail = "创建艺人/乐队失败：{{#requestParam.name}}",
            type = "Performer", subType = "Create",
            bizNo = "{{#bizNo}}", extra = "{{#modifiedData}}")
    @Transactional(rollbackFor = Exception.class)
    public void createPerformer(PerformerSaveReqDTO requestParam) {
        accessControl.requireSuperAdminAccess();
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
        LogRecordContext.putVariable("bizNo", performerDO.getId());
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(performerDO));
    }

    @Override
    @LogRecord(success = "批量导入艺人：总数 {{#importTotal}}，成功 {{#importSuccess}}，失败 {{#importFail}}",
            fail = "批量导入艺人失败", type = "Performer", subType = "Import",
            bizNo = "BATCH_IMPORT", extra = "{{#modifiedData}}")
    public ImportResultRespDTO importPerformers(MultipartFile file) {
        accessControl.requireSuperAdminAccess();
        List<PerformerImportExcelDTO> rows = EasyExcelImportUtil.readFirstSheet(file, PerformerImportExcelDTO.class);
        ImportResultRespDTO result = new ImportResultRespDTO();
        if (rows.isEmpty()) {
            result.addFail(1, "Excel 没有可导入的数据行，请保留表头并从第 2 行开始填写");
            fillImportLogVariables(result);
            return result;
        }
        for (int i = 0; i < rows.size(); i++) {
            int rowIndex = i + 2;
            try {
                PerformerImportExcelDTO row = rows.get(i);
                validatePerformerImportRow(row);
                PerformerSaveReqDTO requestParam = new PerformerSaveReqDTO();
                requestParam.setName(row.getName().trim());
                requestParam.setStyleId(row.getStyleId());
                requestParam.setAvatar(StrUtil.trim(row.getAvatar()));
                requestParam.setBio(StrUtil.trim(row.getBio()));
                requestParam.setStatus(row.getStatus() == null ? 1 : row.getStatus());
                selfProvider.getObject().createPerformer(requestParam);
                result.addSuccess();
            } catch (Exception ex) {
                result.addFail(rowIndex, ex.getMessage());
            }
        }
        fillImportLogVariables(result);
        return result;
    }

    private void fillImportLogVariables(ImportResultRespDTO result) {
        LogRecordContext.putVariable("importTotal", result.getTotal());
        LogRecordContext.putVariable("importSuccess", result.getSuccess());
        LogRecordContext.putVariable("importFail", result.getFail());
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(result));
    }

    private void validatePerformerImportRow(PerformerImportExcelDTO row) {
        if (row == null) {
            throw new ServiceException("空行不能导入");
        }
        if (StrUtil.isBlank(row.getName())) {
            throw new ServiceException("name 不能为空");
        }
        if (row.getStatus() != null && row.getStatus() != 0 && row.getStatus() != 1) {
            throw new ServiceException("status 只能填写 0 或 1");
        }
    }

    @Override
    public IPage<PerformerPageQueryRespDTO> pageQueryPerformers(PerformerPageQueryReqDTO requestParam) {
        accessControl.requireAdminAccess();
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
            
            // 兼容尚未迁移到关联表的存量数据，查询接口保持只读
            if (CollUtil.isEmpty(styleIds) && each.getStyleId() != null) {
                styleIds = new ArrayList<>();
                styleIds.add(each.getStyleId());
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
        accessControl.requireAdminAccess();
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

        // 兼容尚未迁移到关联表的存量数据，查询接口保持只读
        if (CollUtil.isEmpty(styleIds) && performerDO.getStyleId() != null) {
            styleIds = new ArrayList<>();
            styleIds.add(performerDO.getStyleId());
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
    @LogRecord(success = "修改艺人/乐队：艺人ID {{#requestParam.id}}", fail = "修改艺人/乐队失败：艺人ID {{#requestParam.id}}",
            type = "Performer", subType = "Update",
            bizNo = "{{#requestParam.id}}", extra = "{{#modifiedData}}")
    @Transactional(rollbackFor = Exception.class)
    public void updatePerformer(PerformerSaveReqDTO requestParam) {
        accessControl.requireSuperAdminAccess();
        PerformerDO performerDO = getById(requestParam.getId());
        if (performerDO == null) {
            throw new ServiceException("未找到对应的艺人数据");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(performerDO));
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
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(performerDO));
    }

    @Override
    @LogRecord(success = "删除艺人/乐队：艺人ID {{#id}}", fail = "删除艺人/乐队失败：艺人ID {{#id}}",
            type = "Performer", subType = "Delete", bizNo = "{{#id}}")
    @Transactional(rollbackFor = Exception.class)
    public void deletePerformer(Long id) {
        accessControl.requireSuperAdminAccess();
        PerformerDO originalPerformer = getById(id);
        if (originalPerformer == null) {
            throw new ServiceException("未找到对应的艺人数据");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(originalPerformer));
        Long eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_event_performer WHERE performer_id = ?", Long.class, id);
        if (eventCount != null && eventCount > 0) {
            throw new ServiceException("艺人仍有关联演出，不能删除");
        }
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
