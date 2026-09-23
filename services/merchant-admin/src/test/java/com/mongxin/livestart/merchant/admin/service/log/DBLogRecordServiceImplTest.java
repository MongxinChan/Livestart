package com.mongxin.livestart.merchant.admin.service.log;

import com.mongxin.livestart.merchant.admin.dao.entity.OperationLogDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.OperationLogMapper;
import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.context.LogRecordContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DBLogRecordServiceImplTest {

    @BeforeEach
    void initializeLogContext() {
        LogRecordContext.putEmptySpan();
    }

    @AfterEach
    void clearContexts() {
        LogRecordContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldPersistResolvedOperatorAndJsonSnapshots() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("userId", "42");
        request.addHeader("username", "admin-user");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        LogRecordContext.putVariable("originalData", "{\"status\":1}");

        OperationLogMapper mapper = mock(OperationLogMapper.class);
        DBLogRecordServiceImpl service = new DBLogRecordServiceImpl(mapper, new RequestOperatorContext());
        LogRecord record = LogRecord.builder()
                .type("Event")
                .bizNo("1001")
                .operator("42")
                .action("修改演出")
                .extra("{\"status\":2}")
                .build();

        service.record(record);

        ArgumentCaptor<OperationLogDO> captor = ArgumentCaptor.forClass(OperationLogDO.class);
        verify(mapper).insert(captor.capture());
        OperationLogDO saved = captor.getValue();
        assertEquals("42", saved.getOperatorId());
        assertEquals("admin-user", saved.getOperatorName());
        assertEquals("{\"status\":1}", saved.getOriginalData());
        assertEquals("{\"status\":2}", saved.getModifiedData());
        assertEquals(0, saved.getFail());
    }

    @Test
    void shouldQueryPersistedLogs() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        OperationLogDO stored = OperationLogDO.builder()
                .id(9L)
                .tenant("MerchantAdmin")
                .type("Event")
                .subType("Update")
                .bizNo("1001")
                .operatorId("42")
                .operationLog("修改演出")
                .modifiedData("{\"status\":2}")
                .fail(0)
                .build();
        when(mapper.selectList(any())).thenReturn(java.util.List.of(stored));
        DBLogRecordServiceImpl service = new DBLogRecordServiceImpl(mapper, new RequestOperatorContext());

        java.util.List<LogRecord> result = service.queryLogByBizNo("1001", "Event", "Update");

        assertEquals(1, result.size());
        assertEquals("42", result.get(0).getOperator());
        assertEquals("修改演出", result.get(0).getAction());
    }

    @Test
    void shouldPersistAuditLogInIndependentTransaction() throws Exception {
        Method recordMethod = DBLogRecordServiceImpl.class.getMethod("record", LogRecord.class);
        Transactional transactional = recordMethod.getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
