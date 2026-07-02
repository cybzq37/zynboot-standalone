package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.source.SourceRes;
import com.zynboot.map.service.MapSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class SourceController {

    private final MapSourceService sourceService;

    @GetMapping("/layer/{layerId}/source")
    public ApiResponse<List<SourceRes>> listByLayer(@PathVariable String layerId) {
        return ApiResponse.ok(sourceService.listByLayer(layerId));
    }

    @GetMapping("/source/{id}")
    public ApiResponse<SourceRes> getById(@PathVariable String id) {
        return ApiResponse.ok(sourceService.getById(id));
    }

    @DeleteMapping("/source/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        sourceService.delete(id);
        return ApiResponse.ok(null);
    }
}
