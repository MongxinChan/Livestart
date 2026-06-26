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
import com.mongxin.livestart.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuIncreaseStockReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.TicketSkuSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.TicketSkuQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.TicketSkuService;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    private static final String ENGINE_TICKET_STOCK_KEY = "engine:stock:sku:%d";

    private final StringRedisTemplate stringRedisTemplate;
    private final MerchantAdminChainContext merchantAdminChainContext;

    @LogRecord(
            success = """
                    创建票种：{{#requestParam.title}}；
                    关联演出ID：{{#requestParam.eventId}}；
                    售价：{{#requestParam.sellingPrice}}；
                    总库存：{{#requestParam.totalStock}}；
                    单人限购：{{#requestParam.limitNum}};
                    """,
            type = "TicketSku",
            bizNo = "{{#bizNo}}",
            extra = "{{#requestParam.toString()}}"
    )
    @Override
    public void createTicketSku(TicketSkuSaveReqDTO requestParam) {
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

        // 库存缓存预热
        try {
            syncStockCache(ticketSkuDO.getId(), ticketSkuDO.getRemainingStock());
            log.info("票种库存预热成功 | skuId={} | stage1Stock={} | stage2Stock={} | releasedStock={}",
                    ticketSkuDO.getId(), ticketSkuDO.getStage1Stock(), ticketSkuDO.getStage2Stock(), ticketSkuDO.getRemainingStock());
        } catch (Exception e) {
            log.error("票种库存缓存预热失败 | skuId={}", ticketSkuDO.getId(), e);
        }

        // 将运行时生成的票种ID放入日志上下文
        LogRecordContext.putVariable("bizNo", ticketSkuDO.getId());
    }

    @Override
    public List<TicketSkuQueryRespDTO> listByEventId(Long eventId) {
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(TicketSkuDO::getEventId, eventId);
        List<TicketSkuDO> list = list(queryWrapper);
        return list.stream()
                .map(each -> BeanUtil.toBean(each, TicketSkuQueryRespDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public IPage<TicketSkuPageQueryRespDTO> pageQueryTicketSkus(TicketSkuPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(requestParam.getEventId() != null, TicketSkuDO::getEventId, requestParam.getEventId())
                .orderByDesc(TicketSkuDO::getId);
        IPage<TicketSkuDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> BeanUtil.toBean(each, TicketSkuPageQueryRespDTO.class));
    }

    @Override
    public TicketSkuQueryRespDTO getTicketSkuById(Long id) {
        TicketSkuDO ticketSkuDO = getById(id);
        return BeanUtil.toBean(ticketSkuDO, TicketSkuQueryRespDTO.class);
    }

    @LogRecord(
            success = "票种库存增发：票种ID {{#requestParam.skuId}}，增发数量 +{{#requestParam.count}}",
            type = "TicketSku",
            bizNo = "{{#requestParam.skuId}}"
    )
    @Override
    public void increaseStock(TicketSkuIncreaseStockReqDTO requestParam) {
        if (requestParam.getCount() == null || requestParam.getCount() <= 0) {
            throw new ClientException("增发库存数量必须为正整数");
        }

        TicketSkuDO sku = getById(requestParam.getSkuId());
        if (sku == null) {
            throw new ClientException("票种不存在");
        }

        // 保存增发前的原始数据到日志上下文
        LogRecordContext.putVariable("originalData", JSON.toJSONString(sku));

        // 数据库原子增发
        int affected = baseMapper.increaseStock(requestParam.getSkuId(), requestParam.getCount());
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("票种库存增发失败");
        }

        try {
            incrementStockCache(requestParam.getSkuId(), requestParam.getCount());
            log.info("票种库存增发成功 | skuId={} | +{} | DB总库存{}",
                    requestParam.getSkuId(), requestParam.getCount(), sku.getTotalStock() + requestParam.getCount());
        } catch (Exception e) {
            log.error("票种库存缓存增发同步失败（非阻塞）| skuId={}", requestParam.getSkuId(), e);
        }
    }

    @LogRecord(
            success = "删除票种：票种ID {{#id}}",
            type = "TicketSku",
            bizNo = "{{#id}}"
    )
    @Override
    public void deleteTicketSku(Long id) {
        // 保存删除前的原始数据到日志上下文
        TicketSkuDO originalSku = getById(id);
        if (originalSku != null) {
            LogRecordContext.putVariable("originalData", JSON.toJSONString(originalSku));
        }

        removeById(id);

        try {
            deleteStockCache(id);
            log.info("票种删除完成 & 库存缓存已清除 | skuId={}", id);
        } catch (Exception e) {
            log.error("票种库存缓存清除失败（非阻塞）| skuId={}", id, e);
        }
    }

    @LogRecord(
            success = "修改票档：票档ID {{#requestParam.id}}",
            type = "TicketSku",
            bizNo = "{{#requestParam.id}}"
    )
    @Override
    public void updateTicketSku(TicketSkuDO requestParam) {
        TicketSkuDO oldSku = getById(requestParam.getId());
        if (oldSku == null) {
            throw new ClientException("票种不存在");
        }

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
        updateById(oldSku);

        try {
            syncStockCache(oldSku.getId(), oldSku.getRemainingStock());
        } catch (Exception e) {
            log.error("票种库存缓存同步失败（非阻塞）| skuId={}", oldSku.getId(), e);
        }
    }

    private void syncStockCache(Long skuId, Integer stock) {
        String merchantStockCacheKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, skuId);
        String engineStockCacheKey = String.format(ENGINE_TICKET_STOCK_KEY, skuId);
        String stockValue = String.valueOf(stock);
        stringRedisTemplate.opsForValue().set(merchantStockCacheKey, stockValue);
        stringRedisTemplate.opsForValue().set(engineStockCacheKey, stockValue);
    }

    private void incrementStockCache(Long skuId, Integer count) {
        String merchantStockCacheKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, skuId);
        String engineStockCacheKey = String.format(ENGINE_TICKET_STOCK_KEY, skuId);
        stringRedisTemplate.opsForValue().increment(merchantStockCacheKey, count);
        stringRedisTemplate.opsForValue().increment(engineStockCacheKey, count);
    }

    private void deleteStockCache(Long skuId) {
        String merchantStockCacheKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, skuId);
        String engineStockCacheKey = String.format(ENGINE_TICKET_STOCK_KEY, skuId);
        stringRedisTemplate.delete(List.of(merchantStockCacheKey, engineStockCacheKey));
    }
}
