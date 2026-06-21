package com.mongxin.livestart.search.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.search.dto.req.EventSearchReqDTO;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.HotSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;
import com.mongxin.livestart.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
     * 支持关键词、演出类型、城市、价格区间多维度过滤
     */
    @Operation(summary = "搜索演出", description = "根据关键词搜索演唱会、演出项目，支持演出类型、城市、价格区间过滤与分页")
    @GetMapping("/event")
    public Result<IPage<EventSearchRespDTO>> searchEvents(EventSearchReqDTO req) {
        return Results.success(searchService.searchEvents(req));
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
     * 热门搜索推荐（带分值）
     */
    @Operation(summary = "热门搜索", description = "获取当前热门搜索关键词列表，含 Redis ZSet 实时热度分值")
    @GetMapping("/hot")
    public Result<List<HotSearchRespDTO>> hotSearchKeywords() {
        return Results.success(searchService.hotSearchKeywords());
    }

    /**
     * 热搜词点击增分
     * 改为 POST，因为该操作具有副作用（写 Redis），GET 语义不正确
     */
    @Operation(summary = "热搜词点击", description = "用户点击热搜词时调用，在 Redis ZSet 中增加该词的热度分值")
    @PostMapping("/click")
    public Result<Void> clickHotSearch(@RequestParam String keyword) {
        searchService.clickHotSearch(keyword);
        return Results.success();
    }

    /**
     * 搜索建议（autocomplete）
     * 用于导航栏搜索框实时下拉提示，优先热搜词前缀匹配，不足时补充 DB title
     */
    @Operation(summary = "搜索建议", description = "根据输入前缀实时返回搜索建议词，适用于 autocomplete 场景")
    @GetMapping("/suggest")
    public Result<List<String>> suggestKeywords(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") Integer limit) {
        return Results.success(searchService.suggestKeywords(keyword, limit));
    }
}
