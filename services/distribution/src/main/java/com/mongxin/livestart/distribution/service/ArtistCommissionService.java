package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mongxin.livestart.distribution.dao.entity.ArtistCommissionRecordDO;
import com.mongxin.livestart.distribution.dto.resp.ArtistCommissionRespDTO;
import com.mongxin.livestart.distribution.mq.event.CommissionSettleEvent;
import com.mongxin.livestart.distribution.mq.event.OrderPaySuccessEvent;

/**
 * 艺人推广专属宣发码与个税代扣票房提成计算结算服务接口
 */
public interface ArtistCommissionService extends IService<ArtistCommissionRecordDO> {

    /**
     * 获取或生成当前艺人专属的门票推广宣发码及推广总票房和税后分成收益数据
     *
     * @return 艺人宣发码推广情况及收益统计
     */
    com.mongxin.livestart.distribution.dto.resp.InviteCodeRespDTO getOrCreateArtistPromoCode();

    /**
     * 购票成功（普通订单）触发：校验歌迷分销推广链路，自动核算艺人 10% 分成与 20% 个税代扣
     *
     * @param event 支付成功事件
     */
    void processOrderPaySuccess(OrderPaySuccessEvent event);

    /**
     * 自动/延迟 15 天票房个税结算正式到账或取消分成
     *
     * @param event 结算个税通知
     */
    void settleCommission(CommissionSettleEvent event);

    /**
     * 分页查询当前登录艺人的推广提成个税明细列表
     *
     * @param pageNo   页码
     * @param pageSize 页面条数
     * @param status   分成状态筛选
     * @return 票房分成明细分页列表
     */
    IPage<ArtistCommissionRespDTO> pageQueryArtistCommissions(int pageNo, int pageSize, Integer status);
}
