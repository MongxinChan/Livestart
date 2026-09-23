package com.mongxin.livestart.merchant.admin.service;

import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mongxin.livestart.merchant.admin.service.impl.TicketSkuServiceImpl;
import com.mongxin.livestart.merchant.admin.service.security.MerchantAccessControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketSkuServiceImplTest {

    @Mock private StockCacheService stockCacheService;
    private final MerchantAdminChainContext<?> chainContext = new MerchantAdminChainContext<>();
    @Mock private MerchantAccessControl accessControl;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ObjectProvider<TicketSkuService> selfProvider;
    @Mock private TicketSkuMapper ticketSkuMapper;

    @Test
    void shouldRejectConcurrentTicketSkuUpdateWithoutChangingCache() {
        TicketSkuServiceImpl service = new TicketSkuServiceImpl(stockCacheService, chainContext,
                accessControl, jdbcTemplate, selfProvider);
        ReflectionTestUtils.setField(service, "baseMapper", ticketSkuMapper);
        TicketSkuDO current = new TicketSkuDO();
        current.setId(42L);
        current.setTotalStock(10);
        current.setStage1Stock(10);
        current.setStage2Stock(0);
        current.setStage2Released(0);
        current.setRemainingStock(10);
        current.setVersion(1);
        TicketSkuDO request = new TicketSkuDO();
        request.setId(42L);
        when(accessControl.requireTicketSkuAccess(42L)).thenReturn(current);
        when(ticketSkuMapper.updateById(any(TicketSkuDO.class))).thenReturn(0);

        assertThrows(ServiceException.class, () -> service.updateTicketSku(request));

        verify(stockCacheService, never()).adjustAfterCommit(any(), any(Integer.class));
    }
}
