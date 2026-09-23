package com.mongxin.livestart.merchant.admin.service.log;

import com.mongxin.livestart.merchant.admin.LiveStartMerchantAdminApplication;
import com.mongxin.livestart.merchant.admin.service.impl.EventConfigServiceImpl;
import com.mongxin.livestart.merchant.admin.service.impl.EventServiceImpl;
import com.mongxin.livestart.merchant.admin.service.impl.PerformerServiceImpl;
import com.mongxin.livestart.merchant.admin.service.impl.StyleServiceImpl;
import com.mongxin.livestart.merchant.admin.service.impl.TicketSkuServiceImpl;
import com.mongxin.livestart.merchant.admin.service.impl.VenueServiceImpl;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.mzt.logapi.starter.annotation.EnableLogRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OperationLogAnnotationTest {

    @Test
    void shouldRecordOnlyAfterBusinessTransactionCommits() {
        EnableLogRecord annotation = LiveStartMerchantAdminApplication.class
                .getAnnotation(EnableLogRecord.class);

        assertEquals(Ordered.HIGHEST_PRECEDENCE, annotation.order());
    }

    @Test
    void shouldDefineSubtypeAndFailureTemplateForEveryOperationLog() {
        List<Class<?>> serviceTypes = List.of(
                EventServiceImpl.class,
                EventConfigServiceImpl.class,
                TicketSkuServiceImpl.class,
                VenueServiceImpl.class,
                PerformerServiceImpl.class,
                StyleServiceImpl.class);

        int annotationCount = 0;
        for (Class<?> serviceType : serviceTypes) {
            for (Method method : serviceType.getDeclaredMethods()) {
                LogRecord annotation = method.getAnnotation(LogRecord.class);
                if (annotation == null) {
                    continue;
                }
                annotationCount++;
                assertFalse(annotation.subType().isBlank(), method + " 缺少 subType");
                assertFalse(annotation.fail().isBlank(), method + " 缺少失败日志模板");
            }
        }

        assertEquals(24, annotationCount);
    }
}
