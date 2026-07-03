package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.version.LayerVersionRes;
import com.zynboot.map.service.MapQueryFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
@Tag(name = "版本管理", description = "管理图层版本快照与回滚")
public class VersionController {

    private final MapQueryFacadeService queryFacadeService;

    @GetMapping("/layer/{layerId}/version")
    @Operation(summary = "查询图层版本列表")
    public ApiResponse<List<LayerVersionRes>> list(@Parameter(description = "图层 ID") @PathVariable String layerId) {
        return ApiResponse.ok(queryFacadeService.listVersions(layerId));
    }

    @GetMapping("/layer/{layerId}/version/{version}")
    @Operation(summary = "获取指定图层版本")
    public ApiResponse<LayerVersionRes> get(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "版本号", example = "3") @PathVariable Integer version) {
        return ApiResponse.ok(queryFacadeService.getVersion(layerId, version));
    }

    @PostMapping("/layer/{layerId}/version")
    @Operation(summary = "创建图层版本快照")
    public ApiResponse<LayerVersionRes> create(@Parameter(description = "图层 ID") @PathVariable String layerId) {
        return ApiResponse.ok(queryFacadeService.createVersion(layerId));
    }

    @PostMapping("/layer/{layerId}/rollback/{version}")
    @Operation(summary = "回滚图层到指定版本")
    public ApiResponse<Void> rollback(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "版本号", example = "3") @PathVariable Integer version) {
        queryFacadeService.rollback(layerId, version);
        return ApiResponse.ok(null);
    }
}
