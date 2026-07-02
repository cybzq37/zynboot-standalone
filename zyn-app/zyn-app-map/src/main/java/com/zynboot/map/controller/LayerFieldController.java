package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerFieldSaveCmd;
import com.zynboot.map.response.layer.LayerFieldRes;
import com.zynboot.map.service.MapMetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class LayerFieldController {

    private final MapMetadataService metadataService;

    @GetMapping("/layer/{layerId}/field")
    public ApiResponse<List<LayerFieldRes>> list(@PathVariable String layerId) {
        return ApiResponse.ok(metadataService.listFields(layerId));
    }

    @PostMapping("/layer/{layerId}/field")
    public ApiResponse<LayerFieldRes> create(@PathVariable String layerId, @Valid @RequestBody LayerFieldSaveCmd cmd) {
        return ApiResponse.ok(metadataService.createField(layerId, cmd));
    }

    @PutMapping("/field/{id}")
    public ApiResponse<LayerFieldRes> update(@PathVariable String id, @Valid @RequestBody LayerFieldSaveCmd cmd) {
        return ApiResponse.ok(metadataService.updateField(id, cmd));
    }

    @DeleteMapping("/field/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        metadataService.deleteField(id);
        return ApiResponse.ok(null);
    }
}
