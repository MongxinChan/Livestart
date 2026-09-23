package com.mongxin.livestart.merchant.admin.service.security;

import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.VenueMapper;
import com.mongxin.livestart.merchant.admin.service.log.RequestOperatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantAccessControlTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldOnlyAllowVenueAdminToAccessOwnedVenue() {
        VenueMapper venueMapper = mock(VenueMapper.class);
        MerchantAccessControl accessControl = accessControl(venueMapper, mock(EventMapper.class));
        setOperator("42", "3");

        VenueDO ownedVenue = new VenueDO();
        ownedVenue.setId(100L);
        ownedVenue.setOwnerUserId(42L);
        when(venueMapper.selectById(100L)).thenReturn(ownedVenue);
        assertDoesNotThrow(() -> accessControl.requireVenueAccess(100L));

        ownedVenue.setOwnerUserId(99L);
        assertThrows(ClientException.class, () -> accessControl.requireVenueAccess(100L));
    }

    @Test
    void shouldAllowSuperAdminWithoutVenueOwnershipLookup() {
        VenueMapper venueMapper = mock(VenueMapper.class);
        MerchantAccessControl accessControl = accessControl(venueMapper, mock(EventMapper.class));
        setOperator("1", "4");

        assertDoesNotThrow(() -> accessControl.requireVenueAccess(100L));
        verifyNoInteractions(venueMapper);
    }

    @Test
    void shouldRejectEventOwnedByAnotherVenueAdmin() {
        VenueMapper venueMapper = mock(VenueMapper.class);
        EventMapper eventMapper = mock(EventMapper.class);
        MerchantAccessControl accessControl = accessControl(venueMapper, eventMapper);
        setOperator("42", "3");

        EventDO event = new EventDO();
        event.setId(200L);
        event.setVenueId(100L);
        VenueDO venue = new VenueDO();
        venue.setId(100L);
        venue.setOwnerUserId(99L);
        when(eventMapper.selectById(200L)).thenReturn(event);
        when(venueMapper.selectById(100L)).thenReturn(venue);

        assertThrows(ClientException.class, () -> accessControl.requireEventAccess(200L));
    }

    @Test
    void shouldAllowAuthenticatedInternalCallWithoutUserHeaders() {
        VenueMapper venueMapper = mock(VenueMapper.class);
        MerchantAccessControl accessControl = accessControl(venueMapper, mock(EventMapper.class));
        setInternalToken("internal-secret");

        assertDoesNotThrow(() -> accessControl.requireVenueAccess(100L));
        verifyNoInteractions(venueMapper);
    }

    @Test
    void shouldRejectInvalidInternalTokenWithoutAdminRole() {
        MerchantAccessControl accessControl = accessControl(mock(VenueMapper.class), mock(EventMapper.class));
        setInternalToken("wrong-secret");

        assertThrows(ClientException.class, () -> accessControl.requireVenueAccess(100L));
    }

    @Test
    void shouldRestrictMasterDataMutationToSuperAdmin() {
        MerchantAccessControl accessControl = accessControl(mock(VenueMapper.class), mock(EventMapper.class));

        setOperator("42", "3");
        assertThrows(ClientException.class, accessControl::requireSuperAdminAccess);

        setOperator("1", "4");
        assertDoesNotThrow(accessControl::requireSuperAdminAccess);
    }

    private MerchantAccessControl accessControl(VenueMapper venueMapper, EventMapper eventMapper) {
        RequestOperatorContext operatorContext = new RequestOperatorContext();
        ReflectionTestUtils.setField(operatorContext, "internalToken", "internal-secret");
        return new MerchantAccessControl(
                operatorContext, venueMapper, eventMapper, mock(TicketSkuMapper.class));
    }

    private void setOperator(String userId, String userType) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("userId", userId);
        request.addHeader("userType", userType);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void setInternalToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Livestart-Internal-Token", token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
