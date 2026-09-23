package com.mongxin.livestart.merchant.admin.service.log;

import com.mzt.logapi.beans.Operator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestOperatorContextTest {

    private final RequestOperatorContext context = new RequestOperatorContext();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReadAuthenticatedOperatorFromGatewayHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("userId", "2070000000000000001");
        request.addHeader("username", "venue-admin");
        request.addHeader("realName", "场馆管理员");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Operator operator = new RequestOperatorGetService(context).getUser();

        assertEquals("2070000000000000001", operator.getOperatorId());
        assertEquals("场馆管理员", context.getOperatorName());
    }

    @Test
    void shouldFallbackToSystemOutsideHttpRequest() {
        assertEquals("system", context.getOperatorId());
        assertEquals("system", context.getOperatorName());
    }
}
