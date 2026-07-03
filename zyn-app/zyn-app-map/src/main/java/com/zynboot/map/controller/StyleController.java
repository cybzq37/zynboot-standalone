package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerStyleSaveCmd;
import com.zynboot.map.response.layer.LayerStyleRes;
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
@RequestMapping("/map")
@Tag(name = "样式管理", description = "管理图层样式配置")
public class StyleController {

    private final MapMetadataService metadataService;

    @GetMapping("/layer/{layerId}/style")
    @Operation(summary = "查询图层样式列表")
    public ApiResponse<List<LayerStyleRes>> listByLayer(@Parameter(description = "图层 ID") @PathVariable String layerId) {
        return ApiResponse.ok(metadataService.listStyles(layerId));
    }

    @PostMapping("/layer/{layerId}/style")
    @Operation(summary = "创建图层样式")
    public ApiResponse<LayerStyleRes> create(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "样式创建参数", required = true)
            @Valid @RequestBody LayerStyleSaveCmd cmd) {
        return ApiResponse.ok(metadataService.createStyle(layerId, cmd));
    }

    @PutMapping("/style/{id}")
    @Operation(summary = "更新图层样式")
    public ApiResponse<LayerStyleRes> update(
            @Parameter(description = "样式 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "样式更新参数", required = true)
            @Valid @RequestBody LayerStyleSaveCmd cmd) {
        return ApiResponse.ok(metadataService.updateStyle(id, cmd));
    }

    @DeleteMapping("/style/{id}")
    @Operation(summary = "删除图层样式")
    public ApiResponse<Void> delete(@Parameter(description = "样式 ID") @PathVariable String id) {
        metadataService.deleteStyle(id);
        return ApiResponse.ok(null);
    }
}
