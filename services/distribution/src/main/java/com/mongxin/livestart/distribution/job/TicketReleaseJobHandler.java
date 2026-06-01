package com.mongxin.livestart.distribution.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.common.enums.EventStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 演唱会门票定时开售 JobHandler
 * <p>
 * 由 XXL-JOB 调度中心在演出开售时间到达时精准触发（一次性任务），
 * 将对应演出下所有票档的库存从数据库预热至 Redis 缓存，
 * 并更新演出状态为"已开售"，使用户可以开始抢票。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketReleaseJobHandler {

    private final EventMapper eventMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final XxlJobApiService xxlJobApiService;

    @XxlJob("ticketReleaseJobHandler")
    public void execute() {
        String eventIdStr = XxlJobHelper.getJobParam();
        XxlJobHelper.log(">>> 门票开售定时任务触发，eventId={}", eventIdStr);

        if (eventIdStr == null || eventIdStr.isBlank()) {
            XxlJobHelper.handleFail("任务参数为空，缺少 eventId");
            return;
        }

        Long eventId;
        try {
            eventId = Long.parseLong(eventIdStr.trim());
        } catch (NumberFormatException e) {
            XxlJobHelper.handleFail("eventId 解析失败: " + eventIdStr);
            return;
        }

        // 1. 查询演出信息
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            XxlJobHelper.handleFail("未找到演出记录，eventId=" + eventId);
            return;
        }

        if (event.getStatus() != null && event.getStatus() != EventStatusEnum.PENDING_SALE.getCode()) {
            XxlJobHelper.log("演出非待开售状态，跳过处理。当前状态={}", event.getStatus());
            XxlJobHelper.handleSuccess();
            return;
        }

        // 2. 查询该演出下所有票档
        List<TicketSkuDO> skuList = ticketSkuMapper.selectList(
                Wrappers.lambdaQuery(TicketSkuDO.class).eq(TicketSkuDO::getEventId, eventId));

        if (skuList == null || skuList.isEmpty()) {
            XxlJobHelper.handleFail("演出无票档信息，eventId=" + eventId);
            return;
        }

        // 3. 预热 Redis 库存缓存
        int preheatedCount = 0;
        for (TicketSkuDO sku : skuList) {
            String redisKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, sku.getId());
            try {
                stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(sku.getRemainingStock()));
                preheatedCount++;
                XxlJobHelper.log("票档库存预热成功，skuId={}，stock={}，redisKey={}",
                        sku.getId(), sku.getRemainingStock(), redisKey);
            } catch (Exception e) {
                log.error("[门票开售] Redis 预热异常，skuId={}", sku.getId(), e);
                XxlJobHelper.log("票档库存预热异常，skuId={}，error={}", sku.getId(), e.getMessage());
            }
        }

        // 4. 更新演出状态为已开售
        EventDO updateDO = new EventDO();
        updateDO.setId(eventId);
        updateDO.setStatus(EventStatusEnum.ON_SALE.getCode());
        eventMapper.updateById(updateDO);

        String summary = String.format("门票开售任务完成！演出[%s]，预热票档 %d/%d 个",
                event.getTitle(), preheatedCount, skuList.size());
        log.info("[门票开售] {}", summary);
        XxlJobHelper.log(summary);

        // 5. 任务完成后，异步清理该一次性定时任务
        try {
            long jobId = XxlJobHelper.getJobId();
            if (jobId > 0) {
                xxlJobApiService.removeJob((int) jobId);
            }
        } catch (Exception e) {
            log.warn("[门票开售] 清理一次性任务异常（不影响业务）", e);
        }

        XxlJobHelper.handleSuccess();
    }
}
