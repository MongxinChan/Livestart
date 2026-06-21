package com.mongxin.livestart.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.search.dao.entity.EventDO;
import com.mongxin.livestart.search.dao.entity.PerformerDO;
import com.mongxin.livestart.search.dao.mapper.EventMapper;
import com.mongxin.livestart.search.dao.mapper.PerformerMapper;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.HotSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;
import com.mongxin.livestart.search.service.SearchService;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final EventMapper eventMapper;
    private final PerformerMapper performerMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String HOT_SEARCH_KEY = "search:hot_keywords";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public IPage<EventSearchRespDTO> searchEvents(String keyword, Integer pageNum, Integer pageSize) {
        log.info("[搜索] 检索演出，keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);

        // 记录热搜词
        recordHotSearch(keyword);

        Page<EventDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EventDO> queryWrapper = Wrappers.lambdaQuery(EventDO.class);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(EventDO::getTitle, keyword);
        }
        queryWrapper.orderByDesc(EventDO::getId);

        IPage<EventDO> eventPage = eventMapper.selectPage(page, queryWrapper);

        IPage<EventSearchRespDTO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(eventPage.getTotal());
        resultPage.setPages(eventPage.getPages());

        if (eventPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
        } else {
            List<EventSearchRespDTO> list = eventPage.getRecords().stream()
                    .map(this::toEventSearchRespDTO)
                    .collect(Collectors.toList());
            resultPage.setRecords(list);
        }

        return resultPage;
    }

    @Override
    public IPage<PerformerSearchRespDTO> searchPerformers(String keyword, Integer pageNum, Integer pageSize) {
        log.info("[搜索] 检索艺人，keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);

        // 记录热搜词
        recordHotSearch(keyword);

        Page<PerformerDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PerformerDO> queryWrapper = Wrappers.lambdaQuery(PerformerDO.class);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(PerformerDO::getName, keyword);
        }
        queryWrapper.orderByDesc(PerformerDO::getId);

        IPage<PerformerDO> performerPage = performerMapper.selectPage(page, queryWrapper);

        IPage<PerformerSearchRespDTO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(performerPage.getTotal());
        resultPage.setPages(performerPage.getPages());

        if (performerPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
        } else {
            List<PerformerSearchRespDTO> list = performerPage.getRecords().stream()
                    .map(item -> BeanUtil.copyProperties(item, PerformerSearchRespDTO.class))
                    .collect(Collectors.toList());
            resultPage.setRecords(list);
        }

        return resultPage;
    }

    @Override
    public List<HotSearchRespDTO> hotSearchKeywords() {
        // 从 Redis ZSet 获取最高热度的前 10 个词（带 score）
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_SEARCH_KEY, 0, 9);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        return tuples.stream()
                .map(tuple -> new HotSearchRespDTO(
                        tuple.getValue(),
                        tuple.getScore() != null ? tuple.getScore() : 0.0
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void clickHotSearch(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        String cleanKeyword = keyword.trim();
        if (cleanKeyword.length() < 1 || cleanKeyword.length() > 30) {
            return;
        }
        try {
            // 每次点击增加 250 分（与客户端 Mock 逻辑保持一致）
            stringRedisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY, cleanKeyword, 250.0);
            log.info("[搜索] 热搜词点击增分：keyword={}, +250", cleanKeyword);
        } catch (Exception ex) {
            log.error("[搜索] 热搜词点击增分异常，keyword={}", cleanKeyword, ex);
        }
    }

    @Override
    public List<String> suggestKeywords(String keyword, int limit) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        String cleanKeyword = keyword.trim();
        Set<String> results = new LinkedHashSet<>();

        // 1. 从 Redis ZSet 热搜词中前缀匹配（得分高者优先）
        Set<ZSetOperations.TypedTuple<String>> hotTuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_SEARCH_KEY, 0, 49);
        if (hotTuples != null) {
            hotTuples.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .filter(v -> v != null && v.contains(cleanKeyword))
                    .limit(limit)
                    .forEach(results::add);
        }

        // 2. 若热搜未填满，从 DB title 中补充
        if (results.size() < limit) {
            LambdaQueryWrapper<EventDO> queryWrapper = Wrappers.lambdaQuery(EventDO.class)
                    .like(EventDO::getTitle, cleanKeyword)
                    .select(EventDO::getTitle)
                    .last("LIMIT " + limit);
            eventMapper.selectList(queryWrapper).stream()
                    .map(EventDO::getTitle)
                    .filter(t -> !results.contains(t))
                    .limit(limit - results.size())
                    .forEach(results::add);
        }

        return new ArrayList<>(results);
    }

    // ---- 私有方法 ----

    /**
     * EventDO → EventSearchRespDTO 字段转换
     * 负责填充前端 LiveEvent 对齐字段，消除前端的手动映射兜底逻辑
     */
    private EventSearchRespDTO toEventSearchRespDTO(EventDO item) {
        EventSearchRespDTO dto = BeanUtil.copyProperties(item, EventSearchRespDTO.class);
        // 演出类型文本
        dto.setType(item.getEventType() != null && item.getEventType() == 0 ? "Livehouse" : "演唱会");
        // 封面图（直接使用 posterUrl）
        dto.setCover(item.getPosterUrl());
        // 格式化演出时间
        dto.setDate(item.getStartTime() != null ? DATE_FORMAT.format(item.getStartTime()) : "");
        // 场馆（暂用兜底，TODO: 关联 venue 表）
        dto.setVenue("待确认场馆");
        // 艺人（暂留空，TODO: 关联 performer 表）
        dto.setArtist("");
        // 最低价格（暂留 0，TODO: 关联 sku 表）
        dto.setMinPrice(0);
        return dto;
    }

    /**
     * 将合法的关键词记录进 Redis ZSet 实现热度排行
     */
    private void recordHotSearch(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        // 清洗无意义短词或过长长句
        String cleanKeyword = keyword.trim();
        if (cleanKeyword.length() < 2 || cleanKeyword.length() > 20) {
            return;
        }

        try {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY, cleanKeyword, 1.0);
        } catch (Exception ex) {
            log.error("[搜索] 记录热搜词异常，keyword={}", cleanKeyword, ex);
        }
    }
}
