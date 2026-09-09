package com.mongxin.livestart.engine.dao.mapper;

import com.mongxin.livestart.engine.dao.entity.RefundPolicySnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefundPolicyMapper {

    @Select("SELECT e.start_time AS eventStartTime, "
            + "CASE WHEN p.id IS NOT NULL THEN p.is_allow_refund "
            + "ELSE CASE WHEN c.refund_policy_type = 0 THEN 0 ELSE 1 END END AS allowRefund, "
            + "CASE WHEN p.id IS NOT NULL THEN p.tier1_deadline_hours "
            + "ELSE c.tier1_free_refund_hours END AS tier1DeadlineHours, "
            + "CASE WHEN p.id IS NOT NULL THEN p.tier2_deadline_hours "
            + "WHEN c.refund_policy_type = 1 THEN c.tier1_free_refund_hours "
            + "ELSE c.tier2_partial_refund_hours END AS tier2DeadlineHours, "
            + "CASE WHEN p.id IS NOT NULL THEN p.tier2_refund_fee_rate "
            + "WHEN c.refund_policy_type = 1 THEN 0 ELSE c.tier2_refund_fee_rate END AS tier2RefundFeeRate "
            + "FROM t_event e LEFT JOIN t_refund_policy p ON p.event_id = e.id "
            + "LEFT JOIN t_event_config c ON c.event_id = e.id "
            + "WHERE e.id = #{eventId}")
    RefundPolicySnapshot selectByEventId(@Param("eventId") Long eventId);
}
