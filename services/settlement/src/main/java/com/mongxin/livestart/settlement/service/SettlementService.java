package com.mongxin.livestart.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.settlement.dto.resp.SettlementRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementShardRespDTO;
import com.mongxin.livestart.settlement.dto.resp.SettlementStatsRespDTO;

import java.util.List;

public interface SettlementService {

    IPage<SettlementRespDTO> pageSettlements(Long eventId, Integer pageNum, Integer pageSize);

    SettlementRespDTO getSettlementDetail(Long settlementId);

    void triggerSettlement(Long eventId);

    void triggerVisibleSettlements();

    SettlementStatsRespDTO getIncomeStats(Long eventId);

    List<SettlementShardRespDTO> listSettlementShards(Long eventId);
}
