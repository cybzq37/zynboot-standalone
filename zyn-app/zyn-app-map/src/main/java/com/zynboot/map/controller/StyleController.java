package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.layer.LayerStyleSaveCmd;
import com.zynboot.map.response.layer.LayerStyleRes;
import com.zynboot.map.service.MapMetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class StyleController {

    private final MapMetadataService metadataService;

    @GetMapping("/layer/{layerId}/style")
    public ApiResponse<List<LayerStyleRes>> listByLayer(@PathVariable String layerId) {
        return ApiResponse.ok(metadataService.listStyles(layerId));
    }

    @PostMapping("/layer/{layerId}/style")
    public ApiResponse<LayerStyleRes> create(@PathVariable String layerId, @Valid @RequestBody LayerStyleSaveCmd cmd) {
        return ApiResponse.ok(metadataService.createStyle(layerId, cmd));
    }

    @PutMapping("/style/{id}")
    public ApiResponse<LayerStyleRes> update(@PathVariable String id, @Valid @RequestBody LayerStyleSaveCmd cmd) {
        return ApiResponse.ok(metadataService.updateStyle(id, cmd));
    }

    @DeleteMapping("/style/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        metadataService.deleteStyle(id);
        return ApiResponse.ok(null);
    }
}
