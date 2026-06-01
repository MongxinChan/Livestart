package com.mongxin.livestart.search.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;
import com.mongxin.livestart.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索服务 Controller
 */
@Tag(name = "搜索服务")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索演唱会/演出
     */
    @Operation(summary = "搜索演出", description = "根据关键词搜索演唱会、演出项目，支持分页")
    @GetMapping("/event")
    public Result<IPage<EventSearchRespDTO>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Results.success(searchService.searchEvents(keyword, pageNum, pageSize));
    }

    /**
     * 搜索艺人/歌手
     */
    @Operation(summary = "搜索艺人", description = "根据关键词搜索艺人信息")
    @GetMapping("/performer")
    public Result<IPage<PerformerSearchRespDTO>> searchPerformers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Results.success(searchService.searchPerformers(keyword, pageNum, pageSize));
    }

    /**
     * 热门搜索推荐
     */
    @Operation(summary = "热门搜索", description = "获取当前热门搜索关键词列表")
    @GetMapping("/hot")
    public Result<List<String>> hotSearchKeywords() {
        return Results.success(searchService.hotSearchKeywords());
    }
}
