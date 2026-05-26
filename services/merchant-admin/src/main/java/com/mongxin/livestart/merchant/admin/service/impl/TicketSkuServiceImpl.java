package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
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

    private final StringRedisTemplate stringRedisTemplate;
    private final MerchantAdminChainContext merchantAdminChainContext;

    @Override
    public void createTicketSku(TicketSkuSaveReqDTO requestParam) {
        // 通过责任链验证请求参数
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_TICKET_SKU_KEY.name(), requestParam);

        TicketSkuDO ticketSkuDO = BeanUtil.toBean(requestParam, TicketSkuDO.class);
        ticketSkuDO.setRemainingStock(requestParam.getTotalStock());
        save(ticketSkuDO);

        // 库存缓存预热
        try {
            String stockCacheKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, ticketSkuDO.getId());
            stringRedisTemplate.opsForValue().set(stockCacheKey, String.valueOf(ticketSkuDO.getTotalStock()));
            log.info("票种库存预热成功 | skuId={} | stock={}", ticketSkuDO.getId(), ticketSkuDO.getTotalStock());
        } catch (Exception e) {
            log.error("票种库存缓存预热失败 | skuId={}", ticketSkuDO.getId(), e);
        }
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

    @Override
    public void increaseStock(TicketSkuIncreaseStockReqDTO requestParam) {
        if (requestParam.getCount() == null || requestParam.getCount() <= 0) {
            throw new ClientException("增发库存数量必须为正整数");
        }

        TicketSkuDO sku = getById(requestParam.getSkuId());
        if (sku == null) {
            throw new ClientException("票种不存在");
        }

        // 数据库原子增发
        int affected = baseMapper.increaseStock(requestParam.getSkuId(), requestParam.getCount());
        if (!SqlHelper.retBool(affected)) {
            throw new ServiceException("票种库存增发失败");
        }

        // Redis 缓存原子递增
        try {
            String stockCacheKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, requestParam.getSkuId());
            stringRedisTemplate.opsForValue().increment(stockCacheKey, requestParam.getCount());
            log.info("票种库存增发成功 | skuId={} | +{} | DB总库存={}",
                    requestParam.getSkuId(), requestParam.getCount(), sku.getTotalStock() + requestParam.getCount());
        } catch (Exception e) {
            log.error("票种库存缓存增发同步失败（非阻塞） | skuId={}", requestParam.getSkuId(), e);
        }
    }

    @Override
    public void deleteTicketSku(Long id) {
        removeById(id);

        try {
            String stockCacheKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, id);
            stringRedisTemplate.delete(stockCacheKey);
            log.info("票种删除完成 & 库存缓存已清除 | skuId={}", id);
        } catch (Exception e) {
            log.error("票种库存缓存清除失败（非阻塞） | skuId={}", id, e);
        }
    }
}
