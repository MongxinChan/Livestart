package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.dto.req.VenueImportExcelDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenuePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.VenueSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.ImportResultRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenuePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.VenueQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import com.mongxin.livestart.merchant.admin.toolkit.EasyExcelImportUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 场馆服务实现层
 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueDO> implements VenueService {

    @Override
    public void createVenue(VenueSaveReqDTO requestParam) {
        LambdaQueryWrapper<VenueDO> queryWrapper = Wrappers.lambdaQuery(VenueDO.class)
                .eq(VenueDO::getName, requestParam.getName())
                .eq(VenueDO::getCity, requestParam.getCity());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("该城市下已存在同名场馆，请勿重复录入");
        }
        VenueDO venueDO = BeanUtil.toBean(requestParam, VenueDO.class);
        save(venueDO);
    }

    @Override
    public ImportResultRespDTO importVenues(MultipartFile file) {
        List<VenueImportExcelDTO> rows = EasyExcelImportUtil.readFirstSheet(file, VenueImportExcelDTO.class);
        ImportResultRespDTO result = new ImportResultRespDTO();
        if (rows.isEmpty()) {
            result.addFail(1, "Excel 没有可导入的数据行，请保留表头并从第 2 行开始填写");
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
                createVenue(requestParam);
                result.addSuccess();
            } catch (Exception ex) {
                result.addFail(rowIndex, ex.getMessage());
            }
        }
        return result;
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
        IPage<VenueDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, VenuePageQueryRespDTO.class));
    }

    @Override
    public VenueQueryRespDTO getVenueById(Long id) {
        VenueDO venueDO = getById(id);
        return BeanUtil.toBean(venueDO, VenueQueryRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVenue(VenueSaveReqDTO requestParam) {
        if (requestParam.getId() == null) {
            throw new ServiceException("场馆 ID 不能为空");
        }
        if (requestParam.getOwnerUserId() != null) {
            LambdaUpdateWrapper<VenueDO> clearWrapper = Wrappers.lambdaUpdate(VenueDO.class)
                    .eq(VenueDO::getOwnerUserId, requestParam.getOwnerUserId())
                    .ne(VenueDO::getId, requestParam.getId())
                    .set(VenueDO::getOwnerUserId, null);
            baseMapper.update(null, clearWrapper);
        }
        VenueDO venueDO = BeanUtil.toBean(requestParam, VenueDO.class);
        updateById(venueDO);
    }

    @Override
    public void deleteVenue(Long id) {
        removeById(id);
    }
}
