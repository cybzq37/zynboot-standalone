package com.zynboot.map.controller;

import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.service.SpatialAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.zynboot.infra.web.version.ApiVersion;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class SpatialController {

    private final SpatialAnalysisService spatialService;

    @GetMapping("/feature/{id}/buffer")
    public ApiResponse<String> buffer(
            @PathVariable String id,
            @RequestParam double distanceMeters) {
        try {
            return ApiResponse.ok(spatialService.buffer(id, distanceMeters));
        } catch (Exception e) {
            throw BizException.badRequest("缓冲区分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/feature/{id}/area")
    public ApiResponse<Double> area(@PathVariable String id) {
        try {
            return ApiResponse.ok(spatialService.area(id));
        } catch (Exception e) {
            throw BizException.badRequest("面积计算失败: " + e.getMessage());
        }
    }

    @GetMapping("/feature/distance")
    public ApiResponse<Double> distance(
            @RequestParam String featureId1,
            @RequestParam String featureId2) {
        try {
            return ApiResponse.ok(spatialService.distance(featureId1, featureId2));
        } catch (Exception e) {
            throw BizException.badRequest("距离计算失败: " + e.getMessage());
        }
    }

    @GetMapping("/feature/intersects")
    public ApiResponse<Boolean> intersects(
            @RequestParam String featureId1,
            @RequestParam String featureId2) {
        try {
            return ApiResponse.ok(spatialService.intersects(featureId1, featureId2));
        } catch (Exception e) {
            throw BizException.badRequest("空间关系判断失败: " + e.getMessage());
        }
    }

    @GetMapping("/layer/{layerId1}/intersection/{layerId2}")
    public ApiResponse<List<Map<String, Object>>> intersection(
            @PathVariable String layerId1,
            @PathVariable String layerId2) {
        try {
            return ApiResponse.ok(spatialService.intersection(layerId1, layerId2));
        } catch (Exception e) {
            throw BizException.badRequest("叠加分析失败: " + e.getMessage());
        }
    }
}
