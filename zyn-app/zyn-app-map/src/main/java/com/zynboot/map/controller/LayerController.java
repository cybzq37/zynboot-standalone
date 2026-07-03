package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerSaveCmd;
import com.zynboot.map.response.layer.LayerRes;
import com.zynboot.map.service.MapLayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/layer")
@Tag(name = "图层管理", description = "管理矢量图层和栅格图层")
public class LayerController {

    private final MapLayerService layerService;

    @GetMapping
    @Operation(summary = "查询图层列表")
    public ApiResponse<List<LayerRes>> list(
            @Parameter(description = "图层分组 ID，不传时返回全部图层") @RequestParam(required = false) String groupId) {
        return ApiResponse.ok(layerService.list(groupId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取图层详情")
    public ApiResponse<LayerRes> getById(@Parameter(description = "图层 ID") @PathVariable String id) {
        return ApiResponse.ok(layerService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建图层")
    public ApiResponse<Void> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "图层创建参数", required = true)
            @Valid @RequestBody LayerSaveCmd cmd) {
        layerService.create(cmd);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新图层")
    public ApiResponse<Void> update(
            @Parameter(description = "图层 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "图层更新参数", required = true)
            @Valid @RequestBody LayerSaveCmd cmd) {
        layerService.update(id, cmd);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图层")
    public ApiResponse<Void> delete(@Parameter(description = "图层 ID") @PathVariable String id) {
        layerService.delete(id);
        return ApiResponse.ok(null);
    }
}
