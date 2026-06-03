package com.mongxin.livestart.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.HotSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;

import java.util.List;

public interface SearchService {

    IPage<EventSearchRespDTO> searchEvents(String keyword, Integer pageNum, Integer pageSize);

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
}
