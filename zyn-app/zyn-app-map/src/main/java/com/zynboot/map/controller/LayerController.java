package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerSaveCmd;
import com.zynboot.map.response.layer.LayerRes;
import com.zynboot.map.service.MapLayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.zynboot.infra.web.version.ApiVersion;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/layer")
public class LayerController {

    private final MapLayerService layerService;

    @GetMapping
    public ApiResponse<List<LayerRes>> list(@RequestParam(required = false) String groupId) {
        return ApiResponse.ok(layerService.list(groupId));
    }

    @GetMapping("/{id}")
    public ApiResponse<LayerRes> getById(@PathVariable String id) {
        return ApiResponse.ok(layerService.getById(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody LayerSaveCmd cmd) {
        layerService.create(cmd);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody LayerSaveCmd cmd) {
        layerService.update(id, cmd);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        layerService.delete(id);
        return ApiResponse.ok(null);
    }
}
