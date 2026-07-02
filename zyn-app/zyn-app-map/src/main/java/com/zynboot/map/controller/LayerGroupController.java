package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.group.GroupSaveCmd;
import com.zynboot.map.response.group.GroupTreeRes;
import com.zynboot.map.service.MapLayerGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.zynboot.infra.web.version.ApiVersion;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/group")
public class LayerGroupController {

    private final MapLayerGroupService layerGroupService;

    @GetMapping("/tree")
    public ApiResponse<List<GroupTreeRes>> tree() {
        return ApiResponse.ok(layerGroupService.tree());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody GroupSaveCmd cmd) {
        layerGroupService.create(cmd);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody GroupSaveCmd cmd) {
        layerGroupService.update(id, cmd);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        layerGroupService.delete(id);
        return ApiResponse.ok(null);
    }
}
