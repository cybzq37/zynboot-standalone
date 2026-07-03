package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.basemap.BasemapSaveCmd;
import com.zynboot.map.response.basemap.BasemapRes;
import com.zynboot.map.service.MapMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/basemap")
@Tag(name = "底图管理", description = "管理地图实例可用的底图资源")
public class BasemapController {

    private final MapMetadataService metadataService;

    @GetMapping
    @Operation(summary = "查询底图列表")
    public ApiResponse<List<BasemapRes>> list() {
        return ApiResponse.ok(metadataService.listBasemaps());
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认底图")
    public ApiResponse<BasemapRes> getDefault() {
        return ApiResponse.ok(metadataService.getDefaultBasemap());
    }

    @PostMapping
    @Operation(summary = "创建底图")
    public ApiResponse<BasemapRes> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "底图创建参数", required = true)
            @Valid @RequestBody BasemapSaveCmd cmd) {
        return ApiResponse.ok(metadataService.createBasemap(cmd));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新底图")
    public ApiResponse<BasemapRes> update(
            @Parameter(description = "底图 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "底图更新参数", required = true)
            @Valid @RequestBody BasemapSaveCmd cmd) {
        return ApiResponse.ok(metadataService.updateBasemap(id, cmd));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除底图")
    public ApiResponse<Void> delete(@Parameter(description = "底图 ID") @PathVariable String id) {
        metadataService.deleteBasemap(id);
        return ApiResponse.ok(null);
    }
}
