package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dto.req.StylePageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.StyleSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StylePageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.StyleQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.StyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 演出风格管理控制层
 */
@RestController
@RequestMapping("/api/merchant-admin/style")
@RequiredArgsConstructor
@Tag(name = "演出风格管理")
public class StyleController {

    private final StyleService styleService;

    @Operation(summary = "创建风格")
    @PostMapping("/create")
    public Result<Void> createStyle(@RequestBody StyleSaveReqDTO requestParam) {
        styleService.createStyle(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询风格列表")
    @GetMapping("/page")
    public Result<IPage<StylePageQueryRespDTO>> pageQueryStyles(StylePageQueryReqDTO requestParam) {
        return Results.success(styleService.pageQueryStyles(requestParam));
    }

    @Operation(summary = "查询风格详情")
    @GetMapping("/{id}")
    public Result<StyleQueryRespDTO> getStyle(@PathVariable("id") Long id) {
        return Results.success(styleService.getStyleById(id));
    }

    @Operation(summary = "修改风格信息")
    @PutMapping("/update")
    public Result<Void> updateStyle(@RequestBody StyleSaveReqDTO requestParam) {
        styleService.updateStyle(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除风格")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteStyle(@PathVariable("id") Long id) {
        styleService.deleteStyle(id);
        return Results.success();
    }
}
