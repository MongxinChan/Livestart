package com.mongxin.livestart.gateway.filter;

import org.springframework.http.HttpMethod;

/**
 * 后台接口的服务端角色权限策略。
 */
final class RolePermissionPolicy {

    static final int VENUE_ADMIN = 3;
    static final int SUPER_ADMIN = 4;
    static final int FAN = 1;
    static final int ARTIST = 2;

    private RolePermissionPolicy() {
    }

    static boolean isAllowed(String path, HttpMethod method, Integer userType) {
        if (isSuperOnlyPath(path, method)) {
            return Integer.valueOf(SUPER_ADMIN).equals(userType);
        }
        if (isArtistPath(path, method)) {
            if (path.equals("/api/live-start/distribution/v1/artist/bind")) {
                return Integer.valueOf(FAN).equals(userType);
            }
            if (path.endsWith("/complete")) {
                return Integer.valueOf(SUPER_ADMIN).equals(userType);
            }
            return Integer.valueOf(ARTIST).equals(userType);
        }
        if (isAdminPath(path, method)) {
            return Integer.valueOf(VENUE_ADMIN).equals(userType)
                    || Integer.valueOf(SUPER_ADMIN).equals(userType);
        }
        return true;
    }

    private static boolean isArtistPath(String path, HttpMethod method) {
        return path.startsWith("/api/live-start/distribution/v1/artist/");
    }

    private static boolean isSuperOnlyPath(String path, HttpMethod method) {
        boolean masterDataMutation = !HttpMethod.GET.equals(method)
                && (path.startsWith("/api/live-start/merchant-admin/venue/")
                || path.startsWith("/api/live-start/merchant-admin/performer/")
                || path.startsWith("/api/live-start/merchant-admin/style/"));
        return masterDataMutation
                || path.equals("/api/live-start/admin/v1/user/page")
                || path.equals("/api/live-start/admin/v1/user/type")
                || path.equals("/api/live-start/admin/v1/user/status")
                || path.equals("/api/live-start/admin/v1/user/venue-admin");
    }

    private static boolean isAdminPath(String path, HttpMethod method) {
        if (path.startsWith("/api/live-start/merchant-admin/")) {
            return true;
        }
        if (path.startsWith("/api/live-start/settlement/")) {
            return true;
        }
        if (path.startsWith("/api/live-start/engine/order/admin/")) {
            return true;
        }
        if (path.equals("/api/live-start/engine/order/verify/stats")
                || path.equals("/api/live-start/engine/order/verify/records")) {
            return true;
        }
        return HttpMethod.POST.equals(method)
                && path.equals("/api/live-start/engine/order/verify");
    }
}
