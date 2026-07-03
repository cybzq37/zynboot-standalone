package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.source.SourceRes;
import com.zynboot.map.service.MapSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
@Tag(name = "源管理", description = "管理图层下挂接的数据源")
public class SourceController {

    private final MapSourceService sourceService;

    @GetMapping("/layer/{layerId}/source")
    @Operation(summary = "查询图层源列表")
    public ApiResponse<List<SourceRes>> listByLayer(@Parameter(description = "图层 ID") @PathVariable String layerId) {
        return ApiResponse.ok(sourceService.listByLayer(layerId));
    }

    @GetMapping("/source/{id}")
    @Operation(summary = "获取源详情")
    public ApiResponse<SourceRes> getById(@Parameter(description = "源 ID") @PathVariable String id) {
        return ApiResponse.ok(sourceService.getById(id));
    }

    @DeleteMapping("/source/{id}")
    @Operation(summary = "删除源")
    public ApiResponse<Void> delete(@Parameter(description = "源 ID") @PathVariable String id) {
        sourceService.delete(id);
        return ApiResponse.ok(null);
    }
}
