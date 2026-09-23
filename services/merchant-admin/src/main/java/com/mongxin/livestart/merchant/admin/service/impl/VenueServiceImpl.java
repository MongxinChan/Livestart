package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.dto.req.VenueImportExcelDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenuePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenueSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.ImportResultRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenuePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenueQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import com.mongxin.livestart.merchant.admin.service.security.MerchantAccessControl;
import com.mongxin.livestart.merchant.admin.toolkit.EasyExcelImportUtil;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 场馆服务实现层
 */
@Service
@RequiredArgsConstructor
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueDO> implements VenueService {

    private final EventMapper eventMapper;
    private final MerchantAccessControl accessControl;
    private final ObjectProvider<VenueService> selfProvider;

    @Override
    @LogRecord(success = "创建场馆：{{#requestParam.name}}", fail = "创建场馆失败：{{#requestParam.name}}",
            type = "Venue", subType = "Create",
            bizNo = "{{#bizNo}}", extra = "{{#modifiedData}}")
    @Transactional(rollbackFor = Exception.class)
    public void createVenue(VenueSaveReqDTO requestParam) {
        accessControl.requireSuperAdminAccess();
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(VenueDO::getName, requestParam.getName())
                .eq(VenueDO::getCity, requestParam.getCity());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("该城市下已存在同名场馆，请勿重复录入");
        }
        VenueDO venueDO = BeanUtil.toBean(requestParam, VenueDO.class);
        save(venueDO);
        LogRecordContext.putVariable("bizNo", venueDO.getId());
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(venueDO));
    }

    @Override
    @LogRecord(success = "批量导入场馆：总数 {{#importTotal}}，成功 {{#importSuccess}}，失败 {{#importFail}}",
            fail = "批量导入场馆失败", type = "Venue", subType = "Import",
            bizNo = "BATCH_IMPORT", extra = "{{#modifiedData}}")
    public ImportResultRespDTO importVenues(MultipartFile file) {
        accessControl.requireSuperAdminAccess();
        List<VenueImportExcelDTO> rows = EasyExcelImportUtil.readFirstSheet(file, VenueImportExcelDTO.class);
        ImportResultRespDTO result = new ImportResultRespDTO();
        if (rows.isEmpty()) {
            result.addFail(1, "Excel 没有可导入的数据行，请保留表头并从第 2 行开始填写");
            fillImportLogVariables(result);
            return result;
        }
        for (int i = 0; i < rows.size(); i++) {
            int rowIndex = i + 2;
            try {
                VenueImportExcelDTO row = rows.get(i);
                validateVenueImportRow(row);
                VenueSaveReqDTO requestParam = new VenueSaveReqDTO();
                requestParam.setName(row.getName().trim());
                requestParam.setCity(row.getCity().trim());
                requestParam.setAddress(StrUtil.trim(row.getAddress()));
                requestParam.setCapacity(row.getCapacity());
                requestParam.setOwnerUserId(row.getOwnerUserId());
                selfProvider.getObject().createVenue(requestParam);
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

    private void validateVenueImportRow(VenueImportExcelDTO row) {
        if (row == null) {
            throw new ServiceException("空行不能导入");
        }
        if (StrUtil.isBlank(row.getName())) {
            throw new ServiceException("name 不能为空");
        }
        if (StrUtil.isBlank(row.getCity())) {
            throw new ServiceException("city 不能为空");
        }
        if (row.getCapacity() != null && row.getCapacity() < 0) {
            throw new ServiceException("capacity 不能为负数");
        }
    }

    @Override
    public IPage<VenuePageQueryRespDTO> pageQueryVenues(VenuePageQueryReqDTO requestParam) {
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(StrUtil.isNotBlank(requestParam.getCity()), VenueDO::getCity, requestParam.getCity())
                .orderByAsc(VenueDO::getCity)
                .orderByDesc(VenueDO::getId);
        restrictToAccessibleVenues(queryWrapper);
        IPage<VenueDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, VenuePageQueryRespDTO.class));
    }

    @Override
    public VenueQueryRespDTO getVenueById(Long id) {
        accessControl.requireVenueAccess(id);
        VenueDO venueDO = getById(id);
        return BeanUtil.toBean(venueDO, VenueQueryRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(success = "修改场馆：场馆ID {{#requestParam.id}}", fail = "修改场馆失败：场馆ID {{#requestParam.id}}",
            type = "Venue", subType = "Update",
            bizNo = "{{#requestParam.id}}", extra = "{{#modifiedData}}")
    public void updateVenue(VenueSaveReqDTO requestParam) {
        accessControl.requireSuperAdminAccess();
        if (requestParam.getId() == null) {
            throw new ServiceException("场馆 ID 不能为空");
        }
        VenueDO originalVenue = getById(requestParam.getId());
        if (originalVenue == null) {
            throw new ServiceException("场馆不存在");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(originalVenue));
        if (requestParam.getOwnerUserId() != null) {
            LambdaUpdateWrapper<VenueDO> clearWrapper = Wrappers.lambdaUpdate(VenueDO.class)
                    .eq(VenueDO::getOwnerUserId, requestParam.getOwnerUserId())
                    .ne(VenueDO::getId, requestParam.getId())
                    .set(VenueDO::getOwnerUserId, null);
            baseMapper.update(null, clearWrapper);
        }
        VenueDO venueDO = BeanUtil.toBean(requestParam, VenueDO.class);
        updateById(venueDO);
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(getById(requestParam.getId())));
    }

    @Override
    @LogRecord(success = "删除场馆：场馆ID {{#id}}", fail = "删除场馆失败：场馆ID {{#id}}",
            type = "Venue", subType = "Delete", bizNo = "{{#id}}")
    @Transactional(rollbackFor = Exception.class)
    public void deleteVenue(Long id) {
        accessControl.requireSuperAdminAccess();
        VenueDO originalVenue = getById(id);
        if (originalVenue == null) {
            throw new ServiceException("场馆不存在");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(originalVenue));
        Long eventCount = eventMapper.selectCount(Wrappers.lambdaQuery(EventDO.class)
                .eq(EventDO::getVenueId, id));
        if (eventCount > 0) {
            throw new ServiceException("场馆仍有关联演出，不能删除");
        }
        removeById(id);
    }

    private void restrictToAccessibleVenues(LambdaQueryWrapper<VenueDO> queryWrapper) {
        List<Long> venueIds = accessControl.accessibleVenueIds();
        if (venueIds == null) {
            return;
        }
        if (venueIds.isEmpty()) {
            queryWrapper.apply("1 = 0");
            return;
        }
        queryWrapper.in(VenueDO::getId, venueIds);
    }
}
