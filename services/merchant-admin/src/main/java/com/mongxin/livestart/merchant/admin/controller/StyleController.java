package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        styleService.createStyle(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<StyleDO>> listStyles() {
        return Results.success(styleService.listAllStyles());
    }

    /**
     * 分页查询风格列表
     *
     * @param current 当前页码
     * @param size    每页数量
     * @param name    按风格名称模糊搜索（可选）
     */
    @GetMapping("/page")
    public Result<IPage<StyleDO>> pageQueryStyles(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {
        return Results.success(styleService.pageQueryStyles(new Page<>(current, size), name));
    }

    /**
     * 根据 ID 查询风格详情
     */
    @GetMapping("/{id}")
    public Result<StyleDO> getStyle(@PathVariable("id") Long id) {
        return Results.success(styleService.getStyleById(id));
    }

    @PutMapping("/update")
    public Result<Void> updateStyle(@RequestBody StyleDO requestParam) {
        styleService.updateStyle(requestParam);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteStyle(@PathVariable("id") Long id) {
        styleService.deleteStyle(id);
        return Results.success();
    }
}
