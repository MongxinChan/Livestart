package com.mongxin.livestart.distribution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.EventSaleStageDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.EventSaleStageMapper;
import com.mongxin.livestart.distribution.dao.mapper.EventSaleStageSkuMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.dto.req.SaleStageParamDTO;
import com.mongxin.livestart.distribution.dto.req.SaleStageSkuParamDTO;
import com.mongxin.livestart.distribution.dto.req.TicketSkuParam;
import com.mongxin.livestart.distribution.service.impl.EventServiceImpl;
import com.mongxin.livestart.framework.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class EventServiceImplTest {

    @Test
    void resolvesDistributionStageFromMerchantEventId() throws Exception {
        EventMapper eventMapper = mock(EventMapper.class);
        EventSaleStageMapper stageMapper = mock(EventSaleStageMapper.class);
        EventServiceImpl service = new EventServiceImpl(null, stageMapper, null, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", eventMapper);

        long merchantEventId = 101L;
        EventDO distributionEvent = new EventDO();
        distributionEvent.setId(202L);
        EventSaleStageDO stage = new EventSaleStageDO();
        stage.setId(303L);
        stage.setStageName("二开");
        stage.setSaleStartTime(new Date(System.currentTimeMillis() + 60_000));
        when(eventMapper.selectOne(any())).thenReturn(distributionEvent);
        when(stageMapper.selectOne(any())).thenReturn(stage);

        var result = service.getNextSaleStage(merchantEventId);

        assertEquals(202L, result.getEventId());
        assertEquals(303L, result.getId());
        assertEquals("二开", result.getStageName());
        var responseJson = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(result));
        assertEquals("202", responseJson.path("eventId").asText());
        assertEquals("303", responseJson.path("id").asText());
        verify(eventMapper).selectOne(any());
        verify(stageMapper).selectOne(any());
    }

    @Test
    void returnsNoStageWhenEventWasNotPublished() {
        EventMapper eventMapper = mock(EventMapper.class);
        EventServiceImpl service = new EventServiceImpl(null, null, null, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", eventMapper);
        assertNull(service.getNextSaleStage(101L));
    }

    @Test
    void repeatedPublishDoesNotCreateAnotherDistributionEvent() {
        EventMapper eventMapper = mock(EventMapper.class);
        EventServiceImpl service = new EventServiceImpl(null, null, null, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", eventMapper);
        when(eventMapper.selectOne(any())).thenReturn(new EventDO());
        EventPublishReqDTO request = new EventPublishReqDTO();
        request.setSourceEventId(101L);
        request.setSkus(List.of(new TicketSkuParam()));
        request.setSaleStages(List.of(new SaleStageParamDTO()));

        service.publishEvent(request);

        verify(eventMapper).selectOne(any());
        verifyNoMoreInteractions(eventMapper);
    }

    @Test
    void failedSchedulePersistenceDoesNotReportPublishSuccess() {
        EventMapper eventMapper = mock(EventMapper.class);
        TicketSkuMapper skuMapper = mock(TicketSkuMapper.class);
        EventSaleStageMapper stageMapper = mock(EventSaleStageMapper.class);
        EventSaleStageSkuMapper stageSkuMapper = mock(EventSaleStageSkuMapper.class);
        XxlJobApiService jobService = mock(XxlJobApiService.class);
        EventServiceImpl service = new EventServiceImpl(skuMapper, stageMapper, stageSkuMapper, null, jobService);
        ReflectionTestUtils.setField(service, "baseMapper", eventMapper);
        doAnswer(invocation -> {
            ((EventDO) invocation.getArgument(0)).setId(202L);
            return 1;
        }).when(eventMapper).insert(any());
        when(skuMapper.insert(any())).thenReturn(1);
        when(stageMapper.insert(any())).thenReturn(1);
        when(stageSkuMapper.insert(any())).thenReturn(1);
        when(jobService.addTicketReleaseJob(any(), any(), any())).thenReturn(55);

        TicketSkuParam sku = new TicketSkuParam();
        sku.setTitle("A");
        sku.setSellingPrice(BigDecimal.TEN);
        sku.setTotalStock(10);
        SaleStageSkuParamDTO stageSku = new SaleStageSkuParamDTO();
        stageSku.setSkuTitle("A");
        stageSku.setReleaseStock(10);
        SaleStageParamDTO stage = new SaleStageParamDTO();
        stage.setStageNo(1);
        stage.setStageName("首发");
        stage.setSaleStartTime(new Date(System.currentTimeMillis() + 60_000));
        stage.setSkuConfigs(List.of(stageSku));
        EventPublishReqDTO request = new EventPublishReqDTO();
        request.setSourceEventId(101L);
        request.setTitle("测试演出");
        request.setSkus(List.of(sku));
        request.setSaleStages(List.of(stage));

        assertThrows(ServiceException.class, () -> service.publishEvent(request));
        verify(jobService).removeJob(55);
    }
}
