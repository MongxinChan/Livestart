package com.mongxin.livestart.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionPolicyTest {

    @Test
    void shouldOnlyAllowSuperAdminToManageUsersAndMasterData() {
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/admin/v1/user/type", HttpMethod.PUT, RolePermissionPolicy.SUPER_ADMIN));
        assertFalse(RolePermissionPolicy.isAllowed(
                "/api/live-start/admin/v1/user/type", HttpMethod.PUT, RolePermissionPolicy.VENUE_ADMIN));
        assertFalse(RolePermissionPolicy.isAllowed(
                "/api/live-start/merchant-admin/venue/page", HttpMethod.GET, 1));
        assertFalse(RolePermissionPolicy.isAllowed(
                "/api/live-start/merchant-admin/venue/update", HttpMethod.PUT,
                RolePermissionPolicy.VENUE_ADMIN));
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/merchant-admin/venue/update", HttpMethod.PUT,
                RolePermissionPolicy.SUPER_ADMIN));
    }

    @Test
    void shouldAllowBothAdminRolesToUseOperationalEndpoints() {
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/merchant-admin/event/page", HttpMethod.GET, RolePermissionPolicy.VENUE_ADMIN));
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/settlement/list", HttpMethod.GET, RolePermissionPolicy.SUPER_ADMIN));
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/merchant-admin/venue/page", HttpMethod.GET,
                RolePermissionPolicy.VENUE_ADMIN));
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/merchant-admin/performer/page", HttpMethod.GET,
                RolePermissionPolicy.VENUE_ADMIN));
        assertFalse(RolePermissionPolicy.isAllowed(
                "/api/live-start/engine/order/verify", HttpMethod.POST, 1));
    }

    @Test
    void shouldKeepCustomerEndpointsAvailableToAuthenticatedUsers() {
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/engine/order/page", HttpMethod.GET, 1));
        assertTrue(RolePermissionPolicy.isAllowed(
                "/api/live-start/admin/v1/user/me", HttpMethod.GET, 1));
    }
}
