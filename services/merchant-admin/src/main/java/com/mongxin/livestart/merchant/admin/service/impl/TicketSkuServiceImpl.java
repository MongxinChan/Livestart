package com.mongxin.livestart.merchant.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.service.TicketSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 票种服务实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketSkuServiceImpl extends ServiceImpl<TicketSkuMapper, TicketSkuDO> implements TicketSkuService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_CACHE_KEY_PREFIX = "livestart:ticket:stock:%d";

    @Override
    public void createTicketSku(TicketSkuDO requestParam) {
        requestParam.setRemainingStock(requestParam.getTotalStock());
        save(requestParam);

        // 库存缓存预热
        try {
            String stockCacheKey = String.format(STOCK_CACHE_KEY_PREFIX, requestParam.getId());
            stringRedisTemplate.opsForValue().set(stockCacheKey, String.valueOf(requestParam.getTotalStock()));
            log.info("票种库存预热成功 | skuId={} | stock={}", requestParam.getId(), requestParam.getTotalStock());
        } catch (Exception e) {
            log.error("票种库存缓存预热失败 | skuId={}", requestParam.getId(), e);
        }
    }

    @Override
    public List<TicketSkuDO> listByEventId(Long eventId) {
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(TicketSkuDO::getEventId, eventId);
        return list(queryWrapper);
    }

    @Override
    public IPage<TicketSkuDO> pageQueryTicketSkus(Page<TicketSkuDO> page, Long eventId) {
        LambdaQueryWrapper<TicketSkuDO> queryWrapper = Wrappers.lambdaQuery(TicketSkuDO.class)
                .eq(eventId != null, TicketSkuDO::getEventId, eventId)
                .orderByDesc(TicketSkuDO::getId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public TicketSkuDO getTicketSkuById(Long id) {
        return getById(id);
    }

    @Override
    public void increaseStock(Long skuId, Integer count) {
        if (count == null || count <= 0) {
            throw new ClientException("增发库存数量必须为正整数");
        }

        TicketSkuDO sku = getById(skuId);
        if (sku == null) {
            throw new ClientException("票种不存在");
        }

        // 数据库原子增发
        int affected = baseMapper.increaseStock(skuId, count);
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("票种库存增发失败");
        }

        // Redis 缓存原子递增
        try {
            String stockCacheKey = String.format(STOCK_CACHE_KEY_PREFIX, skuId);
            stringRedisTemplate.opsForValue().increment(stockCacheKey, count);
            log.info("票种库存增发成功 | skuId={} | +{} | DB总库存={}", skuId, count, sku.getTotalStock() + count);
        } catch (Exception e) {
            log.error("票种库存缓存增发同步失败（非阻塞） | skuId={}", skuId, e);
        }
    }

    @Override
    public void deleteTicketSku(Long id) {
        removeById(id);

        try {
            String stockCacheKey = String.format(STOCK_CACHE_KEY_PREFIX, id);
            stringRedisTemplate.delete(stockCacheKey);
            log.info("票种删除完成 & 库存缓存已清除 | skuId={}", id);
        } catch (Exception e) {
            log.error("票种库存缓存清除失败（非阻塞） | skuId={}", id, e);
        }
    }
}
