package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuImportExcelDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuIncreaseStockReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.ImportResultRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.TicketSkuService;
import com.mongxin.livestart.merchant.admin.service.StockCacheService;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mongxin.livestart.merchant.admin.service.security.MerchantAccessControl;
import com.mongxin.livestart.merchant.admin.toolkit.EasyExcelImportUtil;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongxin.livestart.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_TICKET_SKU_KEY;

/**
 * 票种服务实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketSkuServiceImpl extends ServiceImpl<TicketSkuMapper, TicketSkuDO> implements TicketSkuService {

    private final StockCacheService stockCacheService;
    private final MerchantAdminChainContext merchantAdminChainContext;
    private final MerchantAccessControl accessControl;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<TicketSkuService> selfProvider;

    @LogRecord(
            success = """
                    创建票种：{{#requestParam.title}}；
                    关联演出ID：{{#requestParam.eventId}}；
                    售价：{{#requestParam.sellingPrice}}；
                    总库存：{{#requestParam.totalStock}}；
                    单人限购：{{#requestParam.limitNum}};
                    """,
            type = "TicketSku",
            subType = "Create",
            fail = "创建票种失败：{{#requestParam.title}}",
            bizNo = "{{#bizNo}}",
            extra = "{{#modifiedData}}"
    )
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTicketSku(TicketSkuSaveReqDTO requestParam) {
        accessControl.requireEventAccess(requestParam.getEventId());
        // 通过责任链验证请求参数
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_TICKET_SKU_KEY.name(), requestParam);

        TicketSkuDO ticketSkuDO = BeanUtil.toBean(requestParam, TicketSkuDO.class);
        int totalStock = requestParam.getTotalStock();
        int stage1Stock = requestParam.getStage1Stock() == null ? totalStock : requestParam.getStage1Stock();
        int stage2Stock = requestParam.getStage2Stock() == null ? Math.max(totalStock - stage1Stock, 0) : requestParam.getStage2Stock();
        if (stage1Stock < 0 || stage2Stock < 0) {
            throw new ClientException("一开和二开放票数量不能为负数");
        }
        if (stage1Stock + stage2Stock > totalStock) {
            throw new ClientException("一开和二开放票数量之和不能超过总库存");
        }
        ticketSkuDO.setStage1Stock(stage1Stock);
        ticketSkuDO.setStage2Stock(stage2Stock);
        ticketSkuDO.setStage2Released(0);
        ticketSkuDO.setRemainingStock(stage1Stock);
        save(ticketSkuDO);

        stockCacheService.initializeAfterCommit(ticketSkuDO.getId(), ticketSkuDO.getRemainingStock());

        // 将运行时生成的票种ID放入日志上下文
        LogRecordContext.putVariable("bizNo", ticketSkuDO.getId());
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(ticketSkuDO));
    }

    @Override
    @LogRecord(
            success = "Excel 批量导入票档：总数 {{#importTotal}}，成功 {{#importSuccess}}，失败 {{#importFail}}",
            fail = "批量导入票档失败",
            type = "TicketSku",
            subType = "Import",
            bizNo = "BATCH_IMPORT",
            extra = "{{#modifiedData}}"
    )
    public ImportResultRespDTO importTicketSkus(MultipartFile file) {
        accessControl.requireAdminAccess();
        List<TicketSkuImportExcelDTO> rows = EasyExcelImportUtil.readFirstSheet(file, TicketSkuImportExcelDTO.class);
        ImportResultRespDTO result = new ImportResultRespDTO();
        if (rows.isEmpty()) {
            result.addFail(1, "Excel 没有可导入的数据行，请保留表头并从第 2 行开始填写");
            fillImportLogVariables(result);
            return result;
        }
        for (int i = 0; i < rows.size(); i++) {
            int rowIndex = i + 2;
            try {
                TicketSkuImportExcelDTO row = rows.get(i);
                validateTicketSkuImportRow(row);
                TicketSkuSaveReqDTO requestParam = new TicketSkuSaveReqDTO();
                requestParam.setEventId(row.getEventId());
                requestParam.setTitle(row.getTitle().trim());
                requestParam.setOriginalPrice(row.getOriginalPrice());
                requestParam.setSellingPrice(row.getSellingPrice());
                requestParam.setTotalStock(row.getTotalStock());
                requestParam.setStage1Stock(row.getStage1Stock());
                requestParam.setStage2Stock(row.getStage2Stock());
                requestParam.setLimitNum(row.getLimitNum());
                selfProvider.getObject().createTicketSku(requestParam);
                result.addSuccess();
            } catch (Exception ex) {
                result.addFail(rowIndex, ex.getMessage());
            }
        }
        fillImportLogVariables(result);
        return result;
    }

    private void validateTicketSkuImportRow(TicketSkuImportExcelDTO row) {
        if (row == null) {
            throw new ClientException("空行不能导入");
        }
        if (row.getEventId() == null) {
            throw new ClientException("eventId 不能为空");
        }
        if (row.getTitle() == null || row.getTitle().isBlank()) {
            throw new ClientException("title 不能为空");
        }
        if (row.getSellingPrice() == null) {
            throw new ClientException("sellingPrice 不能为空");
        }
        if (row.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClientException("sellingPrice 不能为负数");
        }
        if (row.getOriginalPrice() != null && row.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClientException("originalPrice 不能为负数");
        }
        if (row.getTotalStock() == null || row.getTotalStock() <= 0) {
            throw new ClientException("totalStock 必须为正整数");
        }
        if (row.getStage1Stock() != null && row.getStage1Stock() < 0) {
            throw new ClientException("stage1Stock 不能为负数");
        }
        if (row.getStage2Stock() != null && row.getStage2Stock() < 0) {
            throw new ClientException("stage2Stock 不能为负数");
        }
        if (row.getLimitNum() != null && row.getLimitNum() <= 0) {
            throw new ClientException("limitNum 必须为正整数");
        }
    }

    private void fillImportLogVariables(ImportResultRespDTO result) {
        LogRecordContext.putVariable("importTotal", result.getTotal());
        LogRecordContext.putVariable("importSuccess", result.getSuccess());
        LogRecordContext.putVariable("importFail", result.getFail());
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(result));
    }

    @Override
    public List<TicketSkuQueryRespDTO> listByEventId(Long eventId) {
        accessControl.requireEventAccess(eventId);
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(TicketSkuDO::getEventId, eventId);
        List<TicketSkuDO> list = list(queryWrapper);
        return list.stream()
                .map(each -> BeanUtil.toBean(each, TicketSkuQueryRespDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public IPage<TicketSkuPageQueryRespDTO> pageQueryTicketSkus(TicketSkuPageQueryReqDTO requestParam) {
        if (requestParam.getEventId() != null) {
            accessControl.requireEventAccess(requestParam.getEventId());
        }
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(requestParam.getEventId() != null, TicketSkuDO::getEventId, requestParam.getEventId())
                .orderByDesc(TicketSkuDO::getId);
        if (requestParam.getEventId() == null) {
            restrictToAccessibleEvents(queryWrapper);
        }
        IPage<TicketSkuDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, TicketSkuPageQueryRespDTO.class));
    }

    @Override
    public TicketSkuQueryRespDTO getTicketSkuById(Long id) {
        TicketSkuDO ticketSkuDO = accessControl.requireTicketSkuAccess(id);
        return BeanUtil.toBean(ticketSkuDO, TicketSkuQueryRespDTO.class);
    }

    @LogRecord(
            success = "票种库存增发：票种ID {{#requestParam.skuId}}，增发数量 +{{#requestParam.count}}",
            fail = "票种库存增发失败：票种ID {{#requestParam.skuId}}",
            type = "TicketSku",
            subType = "IncreaseStock",
            bizNo = "{{#requestParam.skuId}}",
            extra = "{{#modifiedData}}"
    )
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(TicketSkuIncreaseStockReqDTO requestParam) {
        if (requestParam.getCount() == null || requestParam.getCount() <= 0) {
            throw new ClientException("增发库存数量必须为正整数");
        }

        TicketSkuDO sku = accessControl.requireTicketSkuAccess(requestParam.getSkuId());

        // 保存增发前的原始数据到日志上下文
        LogRecordContext.putVariable("originalData", JSON.toJSONString(sku));

        // 数据库原子增发
        int affected = baseMapper.increaseStock(requestParam.getSkuId(), requestParam.getCount());
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("票种库存增发失败");
        }
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(getById(requestParam.getSkuId())));

        stockCacheService.adjustAfterCommit(requestParam.getSkuId(), requestParam.getCount());
        log.info("票种库存增发成功 | skuId={} | +{} | DB总库存{}",
                requestParam.getSkuId(), requestParam.getCount(), sku.getTotalStock() + requestParam.getCount());
    }

    @LogRecord(
            success = "删除票种：票种ID {{#id}}",
            fail = "删除票种失败：票种ID {{#id}}",
            type = "TicketSku",
            subType = "Delete",
            bizNo = "{{#id}}"
    )
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTicketSku(Long id) {
        // 保存删除前的原始数据到日志上下文
        TicketSkuDO originalSku = accessControl.requireTicketSkuAccess(id);
        LogRecordContext.putVariable("originalData", JSON.toJSONString(originalSku));

        Long orderItemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_order_item WHERE sku_id = ?", Long.class, id);
        if (orderItemCount != null && orderItemCount > 0) {
            throw new ClientException("票档已有订单记录，不能删除");
        }

        jdbcTemplate.update("DELETE FROM t_event_sale_stage_sku WHERE ticket_sku_id = ?", id);

        removeById(id);

        stockCacheService.invalidateAfterCommit(id);
    }

    @LogRecord(
            success = "修改票档：票档ID {{#requestParam.id}}",
            fail = "修改票档失败：票档ID {{#requestParam.id}}",
            type = "TicketSku",
            subType = "Update",
            bizNo = "{{#requestParam.id}}",
            extra = "{{#modifiedData}}"
    )
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTicketSku(TicketSkuDO requestParam) {
        TicketSkuDO oldSku = accessControl.requireTicketSkuAccess(requestParam.getId());
        LogRecordContext.putVariable("originalData", JSON.toJSONString(oldSku));
        int originalRemainingStock = oldSku.getRemainingStock() == null ? 0 : oldSku.getRemainingStock();

        Integer nextStage1Stock = requestParam.getStage1Stock() == null ? oldSku.getStage1Stock() : requestParam.getStage1Stock();
        Integer nextStage2Stock = requestParam.getStage2Stock() == null ? oldSku.getStage2Stock() : requestParam.getStage2Stock();
        int normalizedStage1Stock = nextStage1Stock == null ? 0 : nextStage1Stock;
        int normalizedStage2Stock = nextStage2Stock == null ? 0 : nextStage2Stock;

        if (normalizedStage1Stock < 0 || normalizedStage2Stock < 0) {
            throw new ClientException("一开和二开放票数量不能为负数");
        }
        if (normalizedStage1Stock + normalizedStage2Stock > oldSku.getTotalStock()) {
            throw new ClientException("一开和二开放票数量之和不能超过总库存");
        }

        if ((oldSku.getStage2Released() == null ? 0 : oldSku.getStage2Released()) == 0) {
            int currentStage1Stock = oldSku.getStage1Stock() == null ? 0 : oldSku.getStage1Stock();
            int currentRemainingStock = oldSku.getRemainingStock() == null ? 0 : oldSku.getRemainingStock();
            int soldDuringStage1 = Math.max(currentStage1Stock - currentRemainingStock, 0);
            if (normalizedStage1Stock < soldDuringStage1) {
                throw new ClientException("一开数量不能小于已售出的一开票数");
            }
            oldSku.setRemainingStock(normalizedStage1Stock - soldDuringStage1);
        }

        oldSku.setTitle(requestParam.getTitle());
        oldSku.setOriginalPrice(requestParam.getOriginalPrice());
        oldSku.setSellingPrice(requestParam.getSellingPrice());
        oldSku.setStage1Stock(normalizedStage1Stock);
        oldSku.setStage2Stock(normalizedStage2Stock);
        oldSku.setLimitNum(requestParam.getLimitNum());
        if (!updateById(oldSku)) {
            throw new ServiceException("票档已被其他操作修改，请刷新后重试");
        }
        LogRecordContext.putVariable("modifiedData", JSON.toJSONString(oldSku));
        stockCacheService.adjustAfterCommit(oldSku.getId(), oldSku.getRemainingStock() - originalRemainingStock);
    }

    private void restrictToAccessibleEvents(LambdaQueryWrapper<TicketSkuDO> queryWrapper) {
        List<Long> venueIds = accessControl.accessibleVenueIds();
        if (venueIds == null) {
            return;
        }
        if (venueIds.isEmpty()) {
            queryWrapper.apply("1 = 0");
            return;
        }
        List<Long> eventIds = accessControl.findEventIdsByVenueIds(venueIds);
        if (eventIds.isEmpty()) {
            queryWrapper.apply("1 = 0");
            return;
        }
        queryWrapper.in(TicketSkuDO::getEventId, eventIds);
    }
}
