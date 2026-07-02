package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.instance.InstanceLayerSaveCmd;
import com.zynboot.map.command.instance.InstanceSaveCmd;
import com.zynboot.map.response.instance.InstanceLayerRes;
import com.zynboot.map.response.instance.InstanceRes;
import com.zynboot.map.response.instance.PublishRes;
import com.zynboot.map.service.MapInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class InstanceController {

    private final MapInstanceService instanceService;

    @GetMapping("/instance")
    public ApiResponse<List<InstanceRes>> listInstances() {
        return ApiResponse.ok(instanceService.listInstances());
    }

    @GetMapping("/instance/{id}")
    public ApiResponse<InstanceRes> getInstance(@PathVariable String id) {
        return ApiResponse.ok(instanceService.getInstance(id));
    }

    @PostMapping("/instance")
    public ApiResponse<InstanceRes> createInstance(@Valid @RequestBody InstanceSaveCmd cmd) {
        return ApiResponse.ok(instanceService.createInstance(cmd));
    }

    @PutMapping("/instance/{id}")
    public ApiResponse<InstanceRes> updateInstance(@PathVariable String id, @Valid @RequestBody InstanceSaveCmd cmd) {
        return ApiResponse.ok(instanceService.updateInstance(id, cmd));
    }

    @DeleteMapping("/instance/{id}")
    public ApiResponse<Void> deleteInstance(@PathVariable String id) {
        instanceService.deleteInstance(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/instance/{id}/layers")
    public ApiResponse<List<InstanceLayerRes>> getLayers(@PathVariable String id) {
        return ApiResponse.ok(instanceService.getLayers(id));
    }

    @PutMapping("/instance/{id}/layers")
    public ApiResponse<Void> updateLayers(@PathVariable String id, @RequestBody List<InstanceLayerSaveCmd> layers) {
        instanceService.updateLayers(id, layers);
        return ApiResponse.ok(null);
    }

    @GetMapping("/instance/{id}/publish")
    public ApiResponse<List<PublishRes>> listPublish(@PathVariable String id) {
        return ApiResponse.ok(instanceService.listPublish(id));
    }

    @PostMapping("/instance/{id}/publish")
    public ApiResponse<PublishRes> publish(@PathVariable String id) {
        return ApiResponse.ok(instanceService.publish(id));
    }

    @DeleteMapping("/publish/{id}")
    public ApiResponse<Void> deletePublish(@PathVariable String id) {
        instanceService.deletePublish(id);
        return ApiResponse.ok(null);
    }
}
