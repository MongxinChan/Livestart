package com.mongxin.livestart.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.search.dto.resp.EventSearchRespDTO;
import com.mongxin.livestart.search.dto.resp.PerformerSearchRespDTO;

import java.util.List;

public interface SearchService {

    IPage<EventSearchRespDTO> searchEvents(String keyword, Integer pageNum, Integer pageSize);

    IPage<PerformerSearchRespDTO> searchPerformers(String keyword, Integer pageNum, Integer pageSize);

    List<String> hotSearchKeywords();
}
