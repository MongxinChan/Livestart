package com.mongxin.livestart.distribution.service;

import com.mongxin.livestart.distribution.common.enums.TicketTaskStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.entity.TicketTaskDO;
import com.mongxin.livestart.distribution.dao.entity.UserTicketDO;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketTaskMapper;
import com.mongxin.livestart.distribution.dao.mapper.UserTicketMapper;
import com.mongxin.livestart.distribution.mq.event.TicketTaskExecuteEvent;
import com.mongxin.livestart.distribution.service.impl.TicketTaskExecuteStrategyImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTaskExecuteStrategyImplTest {

    @Mock
    private TicketTaskMapper ticketTaskMapper;
    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private UserTicketMapper userTicketMapper;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private TicketTaskExecuteStrategyImpl strategy;

    @Test
    void missingExcelMustFailWithoutIssuingTickets() {
        TicketTaskDO task = TicketTaskDO.builder()
                .id(1L)
                .ticketSkuId(2L)
                .fileUrl("missing-ticket-list.xlsx")
                .status(TicketTaskStatusEnum.PENDING.getCode())
                .build();
        when(ticketTaskMapper.selectById(1L)).thenReturn(task);
        when(ticketSkuMapper.selectById(2L)).thenReturn(TicketSkuDO.builder()
                .id(2L).eventId(3L).remainingStock(5).build());

        strategy.execute(TicketTaskExecuteEvent.builder().taskId(1L).build());

        assertEquals(TicketTaskStatusEnum.FAILED.getCode(), task.getStatus());
        assertEquals(0, task.getTotalCount());
        verify(userTicketMapper, never()).insert(any(UserTicketDO.class));
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void failedTicketInsertMustThrowForTransactionRollback() {
        when(ticketSkuMapper.update(any(), any())).thenReturn(1);
        when(userTicketMapper.insert(any(UserTicketDO.class))).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> strategy.executeSingleUserDistribution(1L, 2L, 3L));
    }
}
