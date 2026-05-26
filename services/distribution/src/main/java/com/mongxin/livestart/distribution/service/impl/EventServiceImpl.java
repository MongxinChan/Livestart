package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演唱会演出发布及票档管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(EventPublishReqDTO requestParam) {
        // 1. 保存演出基本信息
        EventDO eventDO = EventDO.builder()
                .title(requestParam.getTitle())
                .artistId(requestParam.getArtistId())
                .artistName(requestParam.getArtistName())
                .eventTime(requestParam.getEventTime())
                .address(requestParam.getAddress())
                .build();

        if (!save(eventDO)) {
            throw new ServiceException("演出发布保存失败");
        }

        // 2. 批量保存门票票档库存，并同步初始化 Redis 缓存
        if (CollUtil.isNotEmpty(requestParam.getSkus())) {
            for (EventPublishReqDTO.TicketSkuParam skuParam : requestParam.getSkus()) {
                TicketSkuDO skuDO = TicketSkuDO.builder()
                        .eventId(eventDO.getId())
                        .title(skuParam.getTitle())
                        .originalPrice(skuParam.getSellingPrice()) // 原价与售价一致或原价略高，简化设为一致
                        .sellingPrice(skuParam.getSellingPrice())
                        .totalStock(skuParam.getTotalStock())
                        .remainingStock(skuParam.getTotalStock())
                        .limitNum(skuParam.getLimitNum() != null ? skuParam.getLimitNum() : 2)
                        .version(0)
                        .build();

                int inserted = ticketSkuMapper.insert(skuDO);
                if (inserted <= 0) {
                    throw new ServiceException("票档库存写入失败");
                }

                // 同步初始化抢票高并发 Redis 库存
                String redisKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, skuDO.getId());
                try {
                    stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(skuDO.getRemainingStock()));
                    log.info("[门票发布] 票档库存同步至 Redis 缓存成功，Key={}，Stock={}", redisKey, skuDO.getRemainingStock());
                } catch (Exception e) {
                    log.error("[门票发布] 票档库存同步至 Redis 异常，Key={}", redisKey, e);
                    throw new ServiceException("Redis 缓存服务网络异常，发布终止");
                }
            }
        }

        log.info("[演出发布] 演唱会门票发布成功，eventId={}，主演艺人={}", eventDO.getId(), eventDO.getArtistName());
    }
}
