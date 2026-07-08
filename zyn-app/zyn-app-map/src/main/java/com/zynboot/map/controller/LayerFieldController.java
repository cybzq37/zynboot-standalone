package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerFieldItemCmd;
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
    @Operation(summary = "批量新增图层字段",
            description = "请求体为字段列表（裸数组）。未传 required 时默认 false；required=false 且未传 default_value 时按类型自动初始化（STRING/DATE→NULL，INTEGER→\"0\"，DOUBLE→\"0.0\"，BOOLEAN→\"false\"）。")
    public ApiResponse<List<LayerFieldRes>> createFields(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "字段列表", required = true)
            @Valid @RequestBody List<LayerFieldItemCmd> fields) {
        return ApiResponse.ok(metadataService.createFields(layerId, fields));
    }

    @PutMapping("/layer/{layerId}/field")
    @Operation(summary = "批量更新图层字段（部分更新）",
            description = "请求体为字段列表（裸数组），每个对象必须含 id。仅更新传入的非 null 字段，未传字段保持不变。id 不存在或不属于该图层时返回 400。")
    public ApiResponse<List<LayerFieldRes>> updateFields(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "字段列表（含 id）", required = true)
            @Valid @RequestBody List<LayerFieldItemCmd> fields) {
        return ApiResponse.ok(metadataService.updateFields(layerId, fields));
    }

    @DeleteMapping("/field/{id}")
    @Operation(summary = "删除单个图层字段")
    public ApiResponse<Void> deleteField(@Parameter(description = "字段 ID") @PathVariable String id) {
        metadataService.deleteField(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/layer/{layerId}/field")
    @Operation(summary = "批量删除图层字段",
            description = "请求体为字段 ID 列表（裸字符串数组），仅删除属于该图层的字段。")
    public ApiResponse<Void> deleteFields(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @RequestBody List<String> ids) {
        metadataService.deleteFields(layerId, ids);
        return ApiResponse.ok(null);
    }
}
