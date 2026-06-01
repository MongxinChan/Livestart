package com.mongxin.livestart.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;
import com.mongxin.livestart.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
                    .map(item -> BeanUtil.copyProperties(item, EventSearchRespDTO.class))
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
    public List<String> hotSearchKeywords() {
        // 从 Redis ZSet 获取最高热度的前 10 个词
        Set<String> range = stringRedisTemplate.opsForZSet().reverseRange(HOT_SEARCH_KEY, 0, 9);
        if (range == null || range.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(range);
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
