package com.mongxin.livestart.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.search.dao.entity.EventDO;
import com.mongxin.livestart.search.dao.mapper.EventMapper;
import com.mongxin.livestart.search.dao.mapper.PerformerMapper;
import com.mongxin.livestart.search.dto.req.EventSearchReqDTO;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SearchServiceImpl 单元测试
 * 验证 searchEvents 多维度过滤参数能否正确透传到 EventMapper，并构建分页响应
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private EventMapper eventMapper;
    @Mock
    private PerformerMapper performerMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    private EventDO buildEvent(long id, String title, int type) {
        EventDO event = new EventDO();
        event.setId(id);
        event.setTitle(title);
        event.setEventType(type);
        event.setVenueId(1L);
        event.setStartTime(new Date());
        event.setPosterUrl("https://example.com/poster.jpg");
        event.setStatus(1);
        return event;
    }

    @Test
    void searchEvents_withKeywordOnly_shouldPassKeywordToMapper() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setKeyword("周杰伦");

        when(eventMapper.searchEventsWithFilters(eq("周杰伦"), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(List.of(buildEvent(1L, "周杰伦演唱会", 1)));
        when(eventMapper.countEventsWithFilters(eq("周杰伦"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);

        IPage<EventSearchRespDTO> result = searchService.searchEvents(req);

        assertEquals(1, result.getRecords().size());
        assertEquals("周杰伦演唱会", result.getRecords().get(0).getTitle());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void searchEvents_withEventTypeFilter_shouldPassTypeToMapper() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setEventType(1);

        when(eventMapper.searchEventsWithFilters(any(), eq(1), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(buildEvent(1L, "演唱会A", 1)));
        when(eventMapper.countEventsWithFilters(any(), eq(1), any(), any(), any())).thenReturn(1L);

        IPage<EventSearchRespDTO> result = searchService.searchEvents(req);

        assertEquals(1, result.getRecords().size());
        verify(eventMapper).searchEventsWithFilters(isNull(), eq(1), isNull(), isNull(), isNull(), eq(0), eq(10));
    }

    @Test
    void searchEvents_withCityFilter_shouldPassCityToMapper() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setCity("北京");

        when(eventMapper.searchEventsWithFilters(any(), any(), eq("北京"), any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(eventMapper.countEventsWithFilters(any(), any(), eq("北京"), any(), any())).thenReturn(0L);

        IPage<EventSearchRespDTO> result = searchService.searchEvents(req);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void searchEvents_withPriceRange_shouldPassMinAndMaxToMapper() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setMinPrice(100);
        req.setMaxPrice(500);

        when(eventMapper.searchEventsWithFilters(any(), any(), any(), eq(100), eq(500), anyInt(), anyInt()))
                .thenReturn(List.of(buildEvent(1L, "中价位演出", 0)));
        when(eventMapper.countEventsWithFilters(any(), any(), any(), eq(100), eq(500))).thenReturn(1L);

        IPage<EventSearchRespDTO> result = searchService.searchEvents(req);

        assertEquals(1, result.getRecords().size());
        verify(eventMapper).searchEventsWithFilters(isNull(), isNull(), isNull(), eq(100), eq(500), eq(0), eq(10));
    }

    @Test
    void searchEvents_withAllFilters_shouldPassAllParamsCorrectly() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setKeyword("音乐节");
        req.setEventType(2);
        req.setCity("上海");
        req.setMinPrice(200);
        req.setMaxPrice(800);
        req.setPageNum(2);
        req.setPageSize(5);

        when(eventMapper.searchEventsWithFilters(eq("音乐节"), eq(2), eq("上海"), eq(200), eq(800), eq(5), eq(5)))
                .thenReturn(List.of(buildEvent(1L, "草莓音乐节", 2)));
        when(eventMapper.countEventsWithFilters(eq("音乐节"), eq(2), eq("上海"), eq(200), eq(800))).thenReturn(6L);

        IPage<EventSearchRespDTO> result = searchService.searchEvents(req);

        assertEquals(1, result.getRecords().size());
        assertEquals(6L, result.getTotal());
        assertEquals(2L, result.getPages());
    }

    @Test
    void searchEvents_emptyResult_shouldReturnEmptyPage() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setKeyword("不存在的关键词");

        when(eventMapper.searchEventsWithFilters(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(eventMapper.countEventsWithFilters(any(), any(), any(), any(), any())).thenReturn(0L);

        IPage<EventSearchRespDTO> result = searchService.searchEvents(req);

        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void searchEvents_shouldRecordHotSearchOnlyForValidKeyword() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setKeyword("周杰伦");

        when(eventMapper.searchEventsWithFilters(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(eventMapper.countEventsWithFilters(any(), any(), any(), any(), any())).thenReturn(0L);

        searchService.searchEvents(req);

        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        verify(zSetOperations).incrementScore(anyString(), keywordCaptor.capture(), eq(1.0));
        assertEquals("周杰伦", keywordCaptor.getValue());
    }

    @Test
    void searchEvents_shouldNotRecordHotSearchForBlankKeyword() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setKeyword("  ");

        when(eventMapper.searchEventsWithFilters(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(eventMapper.countEventsWithFilters(any(), any(), any(), any(), any())).thenReturn(0L);

        searchService.searchEvents(req);

        verify(zSetOperations, never()).incrementScore(anyString(), anyString(), anyDouble());
    }

    @Test
    void searchEvents_pageOffsetCalculation_shouldBeCorrect() {
        EventSearchReqDTO req = new EventSearchReqDTO();
        req.setPageNum(3);
        req.setPageSize(20);

        when(eventMapper.searchEventsWithFilters(any(), any(), any(), any(), any(), eq(40), eq(20)))
                .thenReturn(Collections.emptyList());
        when(eventMapper.countEventsWithFilters(any(), any(), any(), any(), any())).thenReturn(100L);

        searchService.searchEvents(req);

        verify(eventMapper).searchEventsWithFilters(isNull(), isNull(), isNull(), isNull(), isNull(), eq(40), eq(20));
    }
}
