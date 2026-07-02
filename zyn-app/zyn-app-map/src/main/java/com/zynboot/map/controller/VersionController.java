package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.version.LayerVersionRes;
import com.zynboot.map.service.MapQueryFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class VersionController {

    private final MapQueryFacadeService queryFacadeService;

    @GetMapping("/layer/{layerId}/version")
    public ApiResponse<List<LayerVersionRes>> list(@PathVariable String layerId) {
        return ApiResponse.ok(queryFacadeService.listVersions(layerId));
    }

    @GetMapping("/layer/{layerId}/version/{version}")
    public ApiResponse<LayerVersionRes> get(@PathVariable String layerId, @PathVariable Integer version) {
        return ApiResponse.ok(queryFacadeService.getVersion(layerId, version));
    }

    @PostMapping("/layer/{layerId}/version")
    public ApiResponse<LayerVersionRes> create(@PathVariable String layerId) {
        return ApiResponse.ok(queryFacadeService.createVersion(layerId));
    }

    @PostMapping("/layer/{layerId}/rollback/{version}")
    public ApiResponse<Void> rollback(@PathVariable String layerId, @PathVariable Integer version) {
        queryFacadeService.rollback(layerId, version);
        return ApiResponse.ok(null);
    }
}
