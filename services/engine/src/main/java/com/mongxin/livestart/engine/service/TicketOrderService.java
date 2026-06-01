package com.mongxin.livestart.engine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPayCallbackReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderRefundReqDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderDetailRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderPageQueryRespDTO;

/**
 * 购票订单服务接口
 */
public interface TicketOrderService {

    /**
     * 购票下单
     *
     * @param requestParam 下单请求参数
     * @return 订单流水号
     */
    String createOrder(TicketOrderCreateReqDTO requestParam);

    /**
     * 支付回调（出票）
     *
     * @param requestParam 支付回调参数
     */
    void payCallback(TicketOrderPayCallbackReqDTO requestParam);

    /**
     * 取消订单
     *
     * @param requestParam 取消请求参数
     */
    void cancelOrder(TicketOrderCancelReqDTO requestParam);

    /**
     * 退票申请
     *
     * @param requestParam 退票申请参数
     */
    void refundOrder(TicketOrderRefundReqDTO requestParam);

    /**
     * 我的订单分页查询
     *
     * @param requestParam 分页查询参数
     * @return 订单分页列表
     */
    IPage<TicketOrderPageQueryRespDTO> pageQueryOrders(TicketOrderPageQueryReqDTO requestParam);

    /**
     * 订单详情查询
     *
     * @param orderNo 订单流水号
     * @return 订单详情
     */
    TicketOrderDetailRespDTO getOrderDetail(String orderNo);
}
