package com.mongxin.livestart.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.search.dto.req.EventSearchReqDTO;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.HotSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;

import java.util.List;

public interface SearchService {

    IPage<EventSearchRespDTO> searchEvents(EventSearchReqDTO req);

    IPage<PerformerSearchRespDTO> searchPerformers(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取热搜关键词排行榜（带分值）
     */
    List<HotSearchRespDTO> hotSearchKeywords();

    /**
     * 热搜词点击增分
     *
     * @param keyword 关键词
     */
    void clickHotSearch(String keyword);

    /**
     * 搜索建议（autocomplete）
     * 优先从 Redis 热搜词前缀匹配，不足时从 DB title 补充
     *
     * @param keyword 输入关键词
     * @param limit   最大返回条数
     * @return 建议词列表
     */
    List<String> suggestKeywords(String keyword, int limit);
}
