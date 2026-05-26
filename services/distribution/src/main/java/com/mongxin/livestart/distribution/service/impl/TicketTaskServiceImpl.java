package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.distribution.common.enums.TicketTaskStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.TicketTaskDO;
import com.mongxin.livestart.distribution.dao.mapper.TicketTaskMapper;
import com.mongxin.livestart.distribution.dto.req.TicketTaskCreateReqDTO;
import com.mongxin.livestart.distribution.mq.event.TicketTaskExecuteEvent;
import com.mongxin.livestart.distribution.mq.producer.TicketTaskProducer;
import com.mongxin.livestart.distribution.service.TicketTaskService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门票大批量分发任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTaskServiceImpl extends ServiceImpl<TicketTaskMapper, TicketTaskDO> implements TicketTaskService {

    private final TicketTaskProducer ticketTaskProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTicketTask(TicketTaskCreateReqDTO requestParam) {
        // 1. 创建赠票任务入库
        TicketTaskDO task = TicketTaskDO.builder()
                .taskName(requestParam.getTaskName())
                .ticketSkuId(requestParam.getTicketSkuId())
                .fileUrl(requestParam.getFileUrl())
                .status(TicketTaskStatusEnum.PENDING.getCode())
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .operator("SYSTEM_ADMIN")
                .build();

        if (!save(task)) {
            throw new ServiceException("大批量赠票任务写入失败");
        }

        log.info("[批量赠票] 推送赠票发票任务已录入，等待异步执行，taskId={}，taskName={}", task.getId(), task.getTaskName());

        // 2. 异步投递 MQ
        TicketTaskExecuteEvent event = TicketTaskExecuteEvent.builder()
                .taskId(task.getId())
                .build();

        try {
            ticketTaskProducer.sendMessage(event);
        } catch (Exception e) {
            log.error("[批量赠票] 推送任务投递 MQ 异常，taskId={}", task.getId(), e);
            throw new ServiceException("投递分发队列异常，请重试");
        }
    }
}
