package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.group.GroupSaveCmd;
import com.zynboot.map.response.group.GroupTreeRes;
import com.zynboot.map.service.MapLayerGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/group")
@Tag(name = "分组管理", description = "管理图层分组树")
public class LayerGroupController {

    private final MapLayerGroupService layerGroupService;

    @GetMapping("/tree")
    @Operation(summary = "获取图层分组树")
    public ApiResponse<List<GroupTreeRes>> tree() {
        return ApiResponse.ok(layerGroupService.tree());
    }

    @PostMapping
    @Operation(summary = "创建图层分组")
    public ApiResponse<Void> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分组创建参数", required = true)
            @Valid @RequestBody GroupSaveCmd cmd) {
        layerGroupService.create(cmd);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新图层分组")
    public ApiResponse<Void> update(
            @Parameter(description = "分组 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "分组更新参数", required = true)
            @Valid @RequestBody GroupSaveCmd cmd) {
        layerGroupService.update(id, cmd);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图层分组")
    public ApiResponse<Void> delete(@Parameter(description = "分组 ID") @PathVariable String id) {
        layerGroupService.delete(id);
        return ApiResponse.ok(null);
    }
}
