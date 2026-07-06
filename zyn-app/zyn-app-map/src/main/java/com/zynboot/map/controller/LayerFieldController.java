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

    @RequestMapping(value = "/layer/{layerId}/field", method = {RequestMethod.POST, RequestMethod.PUT})
    @Operation(summary = "全量替换图层字段",
            description = "请求体为字段完整列表（裸数组）；服务端使 DB 与该列表完全一致——列表中 id 为空的新建、id 非空的更新、DB 中存在但列表里没有的删除。整个操作在一个事务内完成。支持 POST 和 PUT 两种请求方式。")
    public ApiResponse<List<LayerFieldRes>> replaceAll(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "字段全量列表", required = true)
            @Valid @RequestBody List<LayerFieldItemCmd> fields) {
        return ApiResponse.ok(metadataService.replaceFields(layerId, fields));
    }
}
