package com.mongxin.livestart.distribution.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.distribution.common.biz.user.UserContext;
import com.mongxin.livestart.distribution.common.enums.EventSaleStageStatusEnum;
import com.mongxin.livestart.distribution.common.enums.TicketReminderStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.EventSaleStageDO;
import com.mongxin.livestart.distribution.dao.entity.TicketReminderDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.EventSaleStageMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketReminderMapper;
import com.mongxin.livestart.distribution.dto.req.TicketReminderCreateReqDTO;
import com.mongxin.livestart.distribution.dto.resp.TicketReminderRespDTO;
import com.mongxin.livestart.distribution.service.TicketReminderService;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReminderServiceImpl extends ServiceImpl<TicketReminderMapper, TicketReminderDO> implements TicketReminderService {

    private final TicketReminderMapper ticketReminderMapper;
    private final EventMapper eventMapper;
    private final EventSaleStageMapper eventSaleStageMapper;
    private final XxlJobApiService xxlJobApiService;

    @Value("${xxl.job.reminder-lead-minutes:10}")
    private int reminderLeadMinutes;

    @Value("${xxl.job.enabled:true}")
    private boolean xxlJobEnabled;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long subscribeReminder(TicketReminderCreateReqDTO requestParam) {
        Long userId = resolveCurrentUserId();
        EventDO event = eventMapper.selectById(requestParam.getEventId());
        if (event == null) {
            throw new ClientException("Event does not exist");
        }

        EventSaleStageDO saleStage = eventSaleStageMapper.selectById(requestParam.getStageId());
        if (saleStage == null || !event.getId().equals(saleStage.getEventId())) {
            throw new ClientException("Sale stage does not exist");
        }
        if (saleStage.getSaleStartTime() == null) {
            throw new ClientException("This sale stage does not have a sale start time configured");
        }
        if (saleStage.getStatus() != null && saleStage.getStatus() == EventSaleStageStatusEnum.OPENED.getCode()) {
            throw new ClientException("This sale stage is already on sale");
        }
        if (saleStage.getSaleStartTime().before(new Date())) {
            throw new ClientException("This sale stage time has already passed");
        }

        TicketReminderDO existingReminder = ticketReminderMapper.selectOne(
                Wrappers.lambdaQuery(TicketReminderDO.class)
                        .eq(TicketReminderDO::getEventId, requestParam.getEventId())
                        .eq(TicketReminderDO::getStageId, requestParam.getStageId())
                        .eq(TicketReminderDO::getUserId, userId)
                        .eq(TicketReminderDO::getStatus, TicketReminderStatusEnum.PENDING.getCode())
                        .last("limit 1"));
        if (existingReminder != null) {
            return existingReminder.getId();
        }

        Date remindTime = calculateRemindTime(saleStage.getSaleStartTime());
        TicketReminderDO reminder = TicketReminderDO.builder()
                .eventId(event.getId())
                .userId(userId)
                .username(UserContext.getUsername())
                .phone(UserContext.getPhone())
                .eventTitle(event.getTitle())
                .stageId(saleStage.getId())
                .stageNo(saleStage.getStageNo())
                .stageName(saleStage.getStageName())
                .ticketStage(saleStage.getStageNo())
                .saleStartTime(saleStage.getSaleStartTime())
                .remindTime(remindTime)
                .status(TicketReminderStatusEnum.PENDING.getCode())
                .reminderMessage(buildReminderMessage(event, saleStage))
                .build();
        if (ticketReminderMapper.insert(reminder) <= 0) {
            throw new ServiceException("Failed to create reminder subscription");
        }

        Integer jobId = null;
        if (xxlJobEnabled) {
            String cronExpression = dateToCron(remindTime);
            jobId = xxlJobApiService.addTicketReminderJob(reminder.getId(), buildJobTitle(event, saleStage), cronExpression);

            TicketReminderDO updateDO = new TicketReminderDO();
            updateDO.setId(reminder.getId());
            updateDO.setXxlJobId(jobId);
            ticketReminderMapper.updateById(updateDO);
        } else {
            log.warn("[Ticket Reminder] XXL-JOB is disabled in local mode. reminderId={}, eventId={}, stageId={}, remindTime={}",
                    reminder.getId(), event.getId(), saleStage.getId(), remindTime);
        }

        log.info("[Ticket Reminder] Reminder subscribed. reminderId={}, eventId={}, stageId={}, userId={}, remindTime={}, jobId={}",
                reminder.getId(), event.getId(), saleStage.getId(), userId, remindTime, jobId);
        return reminder.getId();
    }

    @Override
    public List<TicketReminderRespDTO> listCurrentUserReminders() {
        Long userId = resolveCurrentUserId();
        List<TicketReminderDO> reminders = ticketReminderMapper.selectList(
                Wrappers.lambdaQuery(TicketReminderDO.class)
                        .eq(TicketReminderDO::getUserId, userId)
                        .orderByDesc(TicketReminderDO::getCreateTime));
        return reminders.stream().map(this::convertToResp).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeReminder(Long reminderId) {
        TicketReminderDO reminder = ticketReminderMapper.selectById(reminderId);
        if (reminder == null) {
            throw new ServiceException("Reminder record does not exist");
        }
        if (!TicketReminderStatusEnum.PENDING.equals(resolveStatus(reminder.getStatus()))) {
            log.info("[Ticket Reminder] Reminder already processed, skip. reminderId={}, status={}", reminderId, reminder.getStatus());
            return;
        }

        TicketReminderDO updateDO = new TicketReminderDO();
        updateDO.setId(reminderId);
        updateDO.setStatus(TicketReminderStatusEnum.REMINDED.getCode());
        updateDO.setReminderMessage(reminder.getReminderMessage());
        ticketReminderMapper.updateById(updateDO);

        log.info("[Ticket Reminder] Reminder delivered. reminderId={}, userId={}, phone={}, eventTitle={}, stageId={}, stageName={}",
                reminderId, reminder.getUserId(), reminder.getPhone(), reminder.getEventTitle(), reminder.getStageId(), reminder.getStageName());
    }

    private TicketReminderRespDTO convertToResp(TicketReminderDO reminder) {
        TicketReminderRespDTO resp = new TicketReminderRespDTO();
        BeanUtils.copyProperties(reminder, resp);
        resp.setStatusDesc(resolveStatus(reminder.getStatus()).getDesc());
        return resp;
    }

    private Long resolveCurrentUserId() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new ClientException("User is not logged in");
        }
        return Long.parseLong(userId);
    }

    private Date calculateRemindTime(Date saleStartTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(saleStartTime);
        calendar.add(Calendar.MINUTE, -reminderLeadMinutes);
        Date remindTime = calendar.getTime();
        Date now = new Date();
        if (remindTime.before(now)) {
            return new Date(now.getTime() + 10_000L);
        }
        return remindTime;
    }

    private String buildReminderMessage(EventDO event, EventSaleStageDO saleStage) {
        return String.format("演出《%s》的%s即将开售，请及时进入抢票页准备下单。", event.getTitle(), saleStage.getStageName());
    }

    private String buildJobTitle(EventDO event, EventSaleStageDO saleStage) {
        return event.getTitle() + " - " + saleStage.getStageName();
    }

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

    private TicketReminderStatusEnum resolveStatus(Integer code) {
        for (TicketReminderStatusEnum each : TicketReminderStatusEnum.values()) {
            if (each.getCode() == (code == null ? -1 : code)) {
                return each;
            }
        }
        return TicketReminderStatusEnum.PENDING;
    }
}
