package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.feature.FeatureSaveCmd;
import com.zynboot.map.response.feature.FeaturePageRes;
import com.zynboot.map.response.feature.FeatureRes;
import com.zynboot.map.service.MapFeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 要素 CRUD + 空间查询 + 聚类。
 * 控制器只负责协议层，查询和写操作全部下沉到应用服务。
 */
@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class LayerFeatureController {

    private final MapFeatureService featureService;

    @GetMapping("/layer/{layerId}/feature")
    public ApiResponse<FeaturePageRes> listByLayer(
            @PathVariable String layerId,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String bbox,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(featureService.listByLayer(layerId, sourceId, bbox, pageNum, pageSize));
    }

    @GetMapping("/layer/{layerId}/search")
    public ApiResponse<FeaturePageRes> search(
            @PathVariable String layerId,
            @RequestParam String query,
            @RequestParam(required = false) String sourceId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(featureService.search(layerId, sourceId, query, pageNum, pageSize));
    }

    @GetMapping("/layer/{layerId}/feature/geojson")
    public ApiResponse<List<Map<String, Object>>> listAsGeoJson(
            @PathVariable String layerId,
            @RequestParam(required = false) String sourceId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.ok(featureService.listAsGeoJson(layerId, sourceId, pageNum, pageSize));
    }

    @GetMapping("/layer/{layerId}/feature/cluster")
    public ApiResponse<List<Map<String, Object>>> cluster(
            @PathVariable String layerId,
            @RequestParam(defaultValue = "10") int k,
            @RequestParam(required = false) String bbox) {
        return ApiResponse.ok(featureService.cluster(layerId, k, bbox));
    }

    @GetMapping("/feature/{id}")
    public ApiResponse<FeatureRes> getById(@PathVariable Long id) {
        return ApiResponse.ok(featureService.getById(id));
    }

    @PostMapping("/layer/{layerId}/feature")
    public ApiResponse<FeatureRes> create(@PathVariable String layerId, @Valid @RequestBody FeatureSaveCmd cmd) {
        return ApiResponse.ok(featureService.create(layerId, cmd));
    }

    @PutMapping("/feature/{id}")
    public ApiResponse<FeatureRes> update(@PathVariable Long id, @Valid @RequestBody FeatureSaveCmd cmd) {
        return ApiResponse.ok(featureService.update(id, cmd));
    }

    @DeleteMapping("/feature/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        featureService.delete(id);
        return ApiResponse.ok(null);
    }
}
