package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.feature.FeatureSaveCmd;
import com.zynboot.map.response.feature.FeaturePageRes;
import com.zynboot.map.response.feature.FeatureRes;
import com.zynboot.map.service.MapFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/map")
@Tag(name = "要素管理", description = "管理图层要素、检索、GeoJSON 输出与聚类分析")
public class LayerFeatureController {

    private final MapFeatureService featureService;

    @GetMapping("/layer/{layerId}/feature")
    @Operation(summary = "分页查询图层要素")
    public ApiResponse<FeaturePageRes> listByLayer(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "来源数据源 ID，不传时返回全部来源数据") @RequestParam(required = false) String sourceId,
            @Parameter(description = "空间过滤框，格式 minx,miny,maxx,maxy") @RequestParam(required = false) String bbox,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(featureService.listByLayer(layerId, sourceId, bbox, pageNum, pageSize));
    }

    @GetMapping("/layer/{layerId}/search")
    @Operation(summary = "按关键字搜索图层要素")
    public ApiResponse<FeaturePageRes> search(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "检索关键字") @RequestParam String query,
            @Parameter(description = "来源数据源 ID，不传时搜索全部来源数据") @RequestParam(required = false) String sourceId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(featureService.search(layerId, sourceId, query, pageNum, pageSize));
    }

    @GetMapping("/layer/{layerId}/feature/geojson")
    @Operation(summary = "按 GeoJSON 结构输出图层要素")
    public ApiResponse<List<Map<String, Object>>> listAsGeoJson(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "来源数据源 ID，不传时输出全部来源数据") @RequestParam(required = false) String sourceId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "100") @RequestParam(defaultValue = "100") int pageSize) {
        return ApiResponse.ok(featureService.listAsGeoJson(layerId, sourceId, pageNum, pageSize));
    }

    @GetMapping("/layer/{layerId}/feature/cluster")
    @Operation(summary = "对图层要素做聚类分析")
    public ApiResponse<List<Map<String, Object>>> cluster(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "聚类簇数", example = "10") @RequestParam(defaultValue = "10") int k,
            @Parameter(description = "空间过滤框，格式 minx,miny,maxx,maxy") @RequestParam(required = false) String bbox) {
        return ApiResponse.ok(featureService.cluster(layerId, k, bbox));
    }

    @GetMapping("/feature/{id}")
    @Operation(summary = "获取要素详情")
    public ApiResponse<FeatureRes> getById(@Parameter(description = "要素 ID") @PathVariable Long id) {
        return ApiResponse.ok(featureService.getById(id));
    }

    @PostMapping("/layer/{layerId}/feature")
    @Operation(summary = "创建要素")
    public ApiResponse<FeatureRes> create(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "要素创建参数", required = true)
            @Valid @RequestBody FeatureSaveCmd cmd) {
        return ApiResponse.ok(featureService.create(layerId, cmd));
    }

    @PutMapping("/feature/{id}")
    @Operation(summary = "更新要素")
    public ApiResponse<FeatureRes> update(
            @Parameter(description = "要素 ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "要素更新参数", required = true)
            @Valid @RequestBody FeatureSaveCmd cmd) {
        return ApiResponse.ok(featureService.update(id, cmd));
    }

    @DeleteMapping("/feature/{id}")
    @Operation(summary = "删除要素")
    public ApiResponse<Void> delete(@Parameter(description = "要素 ID") @PathVariable Long id) {
        featureService.delete(id);
        return ApiResponse.ok(null);
    }
}
