package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.common.enums.EventStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 演唱会演出发布及票档管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final XxlJobApiService xxlJobApiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(EventPublishReqDTO requestParam) {
        // 判断是否设置了开售时间（未设置或已过期则立即开售）
        boolean immediateRelease = requestParam.getSaleStartTime() == null
                || requestParam.getSaleStartTime().before(new Date());

        // 1. 保存演出基本信息
        EventDO eventDO = EventDO.builder()
                .title(requestParam.getTitle())
                .artistId(requestParam.getArtistId())
                .artistName(requestParam.getArtistName())
                .eventTime(requestParam.getEventTime())
                .address(requestParam.getAddress())
                .saleStartTime(requestParam.getSaleStartTime())
                .status(immediateRelease
                        ? EventStatusEnum.ON_SALE.getCode()
                        : EventStatusEnum.PENDING_SALE.getCode())
                .build();

        if (!save(eventDO)) {
            throw new ServiceException("演出发布保存失败");
        }

        // 2. 批量保存门票票档库存
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

                // 仅立即开售时同步预热 Redis 库存；定时开售由 XXL-JOB 在开售时间触发预热
                if (immediateRelease) {
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
        }

        // 3. 定时开售：通过 XXL-JOB OpenAPI 动态注册一次性定时任务
        if (!immediateRelease) {
            try {
                String cronExpression = dateToCron(requestParam.getSaleStartTime());
                int jobId = xxlJobApiService.addTicketReleaseJob(eventDO.getId(), eventDO.getTitle(), cronExpression);

                // 记录 jobId 到演出记录，便于后续管理
                EventDO updateDO = new EventDO();
                updateDO.setId(eventDO.getId());
                updateDO.setXxlJobId(jobId);
                updateById(updateDO);

                log.info("[演出发布] 已注册定时开售任务，eventId={}，jobId={}，开售时间={}",
                        eventDO.getId(), jobId, requestParam.getSaleStartTime());
            } catch (Exception e) {
                log.error("[演出发布] 注册定时开售任务失败，eventId={}", eventDO.getId(), e);
                // 注册失败不回滚演出发布，允许后续手动处理
                log.warn("[演出发布] 定时任务注册失败，可后续在 XXL-JOB 管理后台手动配置");
            }
        }

        log.info("[演出发布] 演唱会门票发布成功，eventId={}，主演艺人={}，开售模式={}",
                eventDO.getId(), eventDO.getArtistName(), immediateRelease ? "立即开售" : "定时开售");
    }

    /**
     * 将 Date 转换为 XXL-JOB Cron 表达式（精确到秒）
     * <p>
     * 格式: 秒 分 时 日 月 ? 年
     * 示例: 0 0 10 15 6 ? 2026
     */
    private String dateToCron(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format("%d %d %d %d %d ? %d",
                cal.get(Calendar.SECOND),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR));
    }
}
