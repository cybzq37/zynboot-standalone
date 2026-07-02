package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.basemap.BasemapSaveCmd;
import com.zynboot.map.response.basemap.BasemapRes;
import com.zynboot.map.service.MapMetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/basemap")
public class BasemapController {

    private final MapMetadataService metadataService;

    @GetMapping
    public ApiResponse<List<BasemapRes>> list() {
        return ApiResponse.ok(metadataService.listBasemaps());
    }

    @GetMapping("/default")
    public ApiResponse<BasemapRes> getDefault() {
        return ApiResponse.ok(metadataService.getDefaultBasemap());
    }

    @PostMapping
    public ApiResponse<BasemapRes> create(@Valid @RequestBody BasemapSaveCmd cmd) {
        return ApiResponse.ok(metadataService.createBasemap(cmd));
    }

    @PutMapping("/{id}")
    public ApiResponse<BasemapRes> update(@PathVariable String id, @Valid @RequestBody BasemapSaveCmd cmd) {
        return ApiResponse.ok(metadataService.updateBasemap(id, cmd));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        metadataService.deleteBasemap(id);
        return ApiResponse.ok(null);
    }
}
