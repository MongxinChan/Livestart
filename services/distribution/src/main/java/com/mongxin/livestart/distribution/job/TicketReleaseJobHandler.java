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
 * One-shot XXL-JOB handler that opens ticket sales at the configured time.
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
        long jobId = XxlJobHelper.getJobId();
        XxlJobHelper.log("Scheduled ticket release triggered. eventId={0}", eventIdStr);

        if (eventIdStr == null || eventIdStr.isBlank()) {
            cleanupCurrentJob(jobId, "missing eventId");
            XxlJobHelper.handleFail("Missing job param: eventId");
            return;
        }

        Long eventId;
        try {
            eventId = Long.parseLong(eventIdStr.trim());
        } catch (NumberFormatException e) {
            cleanupCurrentJob(jobId, "invalid eventId format");
            XxlJobHelper.handleFail("Invalid eventId: " + eventIdStr);
            return;
        }

        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            cleanupCurrentJob(jobId, "event not found");
            XxlJobHelper.handleFail("Event not found. eventId=" + eventId);
            return;
        }

        if (event.getStatus() != null && event.getStatus() != EventStatusEnum.PENDING_SALE.getCode()) {
            XxlJobHelper.log("Event is not in presale status, skip. currentStatus={0}", event.getStatus());
            XxlJobHelper.handleSuccess();
            return;
        }

        List<TicketSkuDO> skuList = ticketSkuMapper.selectList(
                Wrappers.lambdaQuery(TicketSkuDO.class).eq(TicketSkuDO::getEventId, eventId));

        if (skuList == null || skuList.isEmpty()) {
            XxlJobHelper.handleFail("No ticket sku found for event. eventId=" + eventId);
            return;
        }

        int preheatedCount = 0;
        for (TicketSkuDO sku : skuList) {
            String redisKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, sku.getId());
            try {
                stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(sku.getRemainingStock()));
                preheatedCount++;
                XxlJobHelper.log("Preheated sku stock. skuId={0}, stock={1}, redisKey={2}",
                        sku.getId(), sku.getRemainingStock(), redisKey);
            } catch (Exception e) {
                log.error("[Ticket Release] Failed to preheat Redis stock. skuId={}", sku.getId(), e);
                XxlJobHelper.log("Failed to preheat sku stock. skuId={0}, error={1}",
                        sku.getId(), e.getMessage());
            }
        }

        EventDO updateDO = new EventDO();
        updateDO.setId(eventId);
        updateDO.setStatus(EventStatusEnum.ON_SALE.getCode());
        eventMapper.updateById(updateDO);

        String summary = String.format("Ticket release completed. event[%s], preheated sku count %d/%d.",
                event.getTitle(), preheatedCount, skuList.size());
        log.info("[Ticket Release] {}", summary);
        XxlJobHelper.log(summary);

        cleanupCurrentJob(jobId, "ticket release completed");

        XxlJobHelper.handleSuccess();
    }

    private void cleanupCurrentJob(long jobId, String reason) {
        if (jobId <= 0) {
            return;
        }
        try {
            xxlJobApiService.removeJob((int) jobId);
            log.info("[Ticket Release] Cleaned up one-shot xxl-job. jobId={}, reason={}", jobId, reason);
        } catch (Exception e) {
            log.warn("[Ticket Release] Failed to clean up one-shot xxl-job. jobId={}, reason={}", jobId, reason, e);
        }
    }
}
