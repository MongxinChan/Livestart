package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.distribution.dao.entity.TicketTaskDO;
import com.mongxin.livestart.distribution.dto.req.TicketTaskCreateReqDTO;

/**
 * 门票大批量推送分发及赠送任务服务接口
 */
public interface TicketTaskService extends IService<TicketTaskDO> {

    /**
     * 主办方发布大批量推送发票赠送任务，通过 MQ 异步分发
     *
     * @param requestParam 创建推送发票参数
     */
    void createTicketTask(TicketTaskCreateReqDTO requestParam);
}
