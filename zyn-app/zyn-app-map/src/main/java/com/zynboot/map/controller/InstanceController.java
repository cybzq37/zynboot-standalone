package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.instance.InstanceLayerSaveCmd;
import com.zynboot.map.command.instance.InstanceSaveCmd;
import com.zynboot.map.response.instance.InstanceLayerRes;
import com.zynboot.map.response.instance.InstanceRes;
import com.zynboot.map.response.instance.PublishRes;
import com.zynboot.map.service.MapInstanceService;
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
@Tag(name = "地图实例", description = "管理地图实例、图层树和发布记录")
public class InstanceController {

    private final MapInstanceService instanceService;

    @GetMapping("/instance")
    @Operation(summary = "查询地图实例列表")
    public ApiResponse<List<InstanceRes>> listInstances() {
        return ApiResponse.ok(instanceService.listInstances());
    }

    @GetMapping("/instance/{id}")
    @Operation(summary = "获取地图实例详情")
    public ApiResponse<InstanceRes> getInstance(@Parameter(description = "实例 ID") @PathVariable String id) {
        return ApiResponse.ok(instanceService.getInstance(id));
    }

    @PostMapping("/instance")
    @Operation(summary = "创建地图实例")
    public ApiResponse<InstanceRes> createInstance(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "地图实例创建参数", required = true)
            @Valid @RequestBody InstanceSaveCmd cmd) {
        return ApiResponse.ok(instanceService.createInstance(cmd));
    }

    @PutMapping("/instance/{id}")
    @Operation(summary = "更新地图实例")
    public ApiResponse<InstanceRes> updateInstance(
            @Parameter(description = "实例 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "地图实例更新参数", required = true)
            @Valid @RequestBody InstanceSaveCmd cmd) {
        return ApiResponse.ok(instanceService.updateInstance(id, cmd));
    }

    @DeleteMapping("/instance/{id}")
    @Operation(summary = "删除地图实例")
    public ApiResponse<Void> deleteInstance(@Parameter(description = "实例 ID") @PathVariable String id) {
        instanceService.deleteInstance(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/instance/{id}/layers")
    @Operation(summary = "获取实例图层树")
    public ApiResponse<List<InstanceLayerRes>> getLayers(@Parameter(description = "实例 ID") @PathVariable String id) {
        return ApiResponse.ok(instanceService.getLayers(id));
    }

    @PutMapping("/instance/{id}/layers")
    @Operation(summary = "更新实例图层树")
    public ApiResponse<Void> updateLayers(
            @Parameter(description = "实例 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "实例图层树节点列表", required = true)
            @RequestBody List<InstanceLayerSaveCmd> layers) {
        instanceService.updateLayers(id, layers);
        return ApiResponse.ok(null);
    }

    @GetMapping("/instance/{id}/publish")
    @Operation(summary = "查询实例发布记录")
    public ApiResponse<List<PublishRes>> listPublish(@Parameter(description = "实例 ID") @PathVariable String id) {
        return ApiResponse.ok(instanceService.listPublish(id));
    }

    @PostMapping("/instance/{id}/publish")
    @Operation(summary = "发布地图实例")
    public ApiResponse<PublishRes> publish(@Parameter(description = "实例 ID") @PathVariable String id) {
        return ApiResponse.ok(instanceService.publish(id));
    }

    @DeleteMapping("/publish/{id}")
    @Operation(summary = "删除发布记录")
    public ApiResponse<Void> deletePublish(@Parameter(description = "发布记录 ID") @PathVariable String id) {
        instanceService.deletePublish(id);
        return ApiResponse.ok(null);
    }
}
