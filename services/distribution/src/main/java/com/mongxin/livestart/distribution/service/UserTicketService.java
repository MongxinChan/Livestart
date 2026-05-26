package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.distribution.dao.entity.UserTicketDO;
import com.mongxin.livestart.distribution.dto.req.TicketGrabReqDTO;
import com.mongxin.livestart.distribution.dto.resp.UserTicketRespDTO;

/**
 * 歌迷电子门票秒杀抢购与检索服务接口
 */
public interface UserTicketService extends IService<UserTicketDO> {

    /**
     * 歌迷高并发秒杀/抢领特权特价门票 (核心抢票链路，Lua + Redis 控制库存与限领)
     *
     * @param requestParam 抢购秒杀请求参数
     */
    void grabTicket(TicketGrabReqDTO requestParam);

    /**
     * 分页查询当前登录歌迷拥有的演出电子门票
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param status   状态筛选 (0:未使用 1:已使用 2:已退票)
     * @return 分页门票信息
     */
    IPage<UserTicketRespDTO> pageQueryUserTickets(int pageNo, int pageSize, Integer status);
}
