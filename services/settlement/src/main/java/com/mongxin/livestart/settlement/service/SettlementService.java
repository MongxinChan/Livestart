package com.mongxin.livestart.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.settlement.dto.resp.SettlementRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementStatsRespDTO;

public interface SettlementService {

    IPage<SettlementRespDTO> pageSettlements(Long eventId, Integer pageNum, Integer pageSize);

    SettlementRespDTO getSettlementDetail(Long settlementId);

    void triggerSettlement(Long eventId);

    SettlementStatsRespDTO getIncomeStats(Long eventId);
}
