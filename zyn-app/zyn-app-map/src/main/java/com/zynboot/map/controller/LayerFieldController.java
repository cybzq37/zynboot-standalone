package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerFieldSaveCmd;
import com.zynboot.map.response.layer.LayerFieldRes;
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
@Tag(name = "字段管理", description = "管理图层属性字段元数据")
public class LayerFieldController {

    private final MapMetadataService metadataService;

    @GetMapping("/layer/{layerId}/field")
    @Operation(summary = "查询图层字段列表")
    public ApiResponse<List<LayerFieldRes>> list(@Parameter(description = "图层 ID") @PathVariable String layerId) {
        return ApiResponse.ok(metadataService.listFields(layerId));
    }

    @PostMapping("/layer/{layerId}/field")
    @Operation(summary = "创建图层字段")
    public ApiResponse<LayerFieldRes> create(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "字段创建参数", required = true)
            @Valid @RequestBody LayerFieldSaveCmd cmd) {
        return ApiResponse.ok(metadataService.createField(layerId, cmd));
    }

    @PutMapping("/field/{id}")
    @Operation(summary = "更新图层字段")
    public ApiResponse<LayerFieldRes> update(
            @Parameter(description = "字段 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "字段更新参数", required = true)
            @Valid @RequestBody LayerFieldSaveCmd cmd) {
        return ApiResponse.ok(metadataService.updateField(id, cmd));
    }

    @DeleteMapping("/field/{id}")
    @Operation(summary = "删除图层字段")
    public ApiResponse<Void> delete(@Parameter(description = "字段 ID") @PathVariable String id) {
        metadataService.deleteField(id);
        return ApiResponse.ok(null);
    }
}
