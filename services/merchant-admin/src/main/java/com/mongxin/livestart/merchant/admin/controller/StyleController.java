package com.mongxin.livestart.merchant.admin.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.service.StyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-admin/style")
@RequiredArgsConstructor
public class StyleController {

    private final StyleService styleService;

    @PostMapping("/create")
    public Result<Void> createStyle(@RequestBody StyleDO requestParam) {
        styleService.save(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<StyleDO>> listStyles() {
        return Results.success(styleService.list());
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteStyle(@PathVariable("id") Long id) {
        styleService.removeById(id);
        return Results.success();
    }
}
