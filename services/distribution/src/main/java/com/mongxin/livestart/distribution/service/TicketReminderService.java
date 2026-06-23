package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.distribution.dao.entity.TicketReminderDO;
import com.mongxin.livestart.distribution.dto.req.TicketReminderCreateReqDTO;
import com.mongxin.livestart.distribution.dto.resp.TicketReminderRespDTO;

import java.util.List;

public interface TicketReminderService extends IService<TicketReminderDO> {

    Long subscribeReminder(TicketReminderCreateReqDTO requestParam);

    List<TicketReminderRespDTO> listCurrentUserReminders();

    void executeReminder(Long reminderId);
}
