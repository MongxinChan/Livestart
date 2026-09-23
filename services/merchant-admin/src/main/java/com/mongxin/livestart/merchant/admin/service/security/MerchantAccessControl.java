package com.mongxin.livestart.merchant.admin.service.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.service.log.RequestOperatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 商户后台的数据权限校验。角色权限由网关先行拦截，这里负责场馆归属。
 */
@Component
@RequiredArgsConstructor
public class MerchantAccessControl {

    private static final int VENUE_ADMIN = 3;
    private static final int SUPER_ADMIN = 4;

    private final RequestOperatorContext operatorContext;
    private final VenueMapper venueMapper;
    private final EventMapper eventMapper;
    private final TicketSkuMapper ticketSkuMapper;

    public boolean isSuperAdmin() {
        return operatorContext.isInternalCall()
                || Integer.valueOf(SUPER_ADMIN).equals(operatorContext.getUserType());
    }

    public void requireAdminAccess() {
        requireAdminRole();
    }

    public void requireSuperAdminAccess() {
        requireAdminRole();
        if (!isSuperAdmin()) {
            throw new ClientException("当前账号无权维护平台基础数据");
        }
    }

    public List<Long> accessibleVenueIds() {
        requireAdminRole();
        if (isSuperAdmin()) {
            return null;
        }
        Long userId = currentUserId();
        return venueMapper.selectList(Wrappers.lambdaQuery(VenueDO.class)
                        .eq(VenueDO::getOwnerUserId, userId))
                .stream()
                .map(VenueDO::getId)
                .toList();
    }

    public void requireVenueAccess(Long venueId) {
        requireAdminRole();
        if (isSuperAdmin()) {
            return;
        }
        VenueDO venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new ClientException("场馆不存在");
        }
        if (!Objects.equals(venue.getOwnerUserId(), currentUserId())) {
            throw new ClientException("无权操作其他场馆的数据");
        }
    }

    public EventDO requireEventAccess(Long eventId) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        requireVenueAccess(event.getVenueId());
        return event;
    }

    public TicketSkuDO requireTicketSkuAccess(Long skuId) {
        TicketSkuDO sku = ticketSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new ClientException("票种不存在");
        }
        requireEventAccess(sku.getEventId());
        return sku;
    }

    public List<Long> findEventIdsByVenueIds(List<Long> venueIds) {
        if (venueIds == null || venueIds.isEmpty()) {
            return List.of();
        }
        return eventMapper.selectList(Wrappers.lambdaQuery(EventDO.class)
                        .in(EventDO::getVenueId, venueIds))
                .stream()
                .map(EventDO::getId)
                .toList();
    }

    private void requireAdminRole() {
        if (operatorContext.isInternalCall()) {
            return;
        }
        Integer userType = operatorContext.getUserType();
        if (!Integer.valueOf(VENUE_ADMIN).equals(userType)
                && !Integer.valueOf(SUPER_ADMIN).equals(userType)) {
            throw new ClientException("当前账号无权访问商户后台");
        }
    }

    private Long currentUserId() {
        try {
            return Long.valueOf(operatorContext.getOperatorId());
        } catch (NumberFormatException ex) {
            throw new ClientException("当前操作人身份无效");
        }
    }
}
