package com.zynboot.map.controller;

import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.service.SpatialAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
@Tag(name = "空间分析", description = "提供缓冲区、面积、距离、相交等空间分析接口")
public class SpatialController {

    private final SpatialAnalysisService spatialService;

    @GetMapping("/feature/{id}/buffer")
    @Operation(summary = "计算要素缓冲区")
    public ApiResponse<String> buffer(
            @Parameter(description = "要素 ID") @PathVariable String id,
            @Parameter(description = "缓冲距离，单位米", example = "100") @RequestParam double distanceMeters) {
        try {
            return ApiResponse.ok(spatialService.buffer(id, distanceMeters));
        } catch (Exception e) {
            throw BizException.badRequest("缓冲区分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/feature/{id}/area")
    @Operation(summary = "计算要素面积")
    public ApiResponse<Double> area(@Parameter(description = "要素 ID") @PathVariable String id) {
        try {
            return ApiResponse.ok(spatialService.area(id));
        } catch (Exception e) {
            throw BizException.badRequest("面积计算失败: " + e.getMessage());
        }
    }

    @GetMapping("/feature/distance")
    @Operation(summary = "计算两个要素距离")
    public ApiResponse<Double> distance(
            @Parameter(description = "起始要素 ID") @RequestParam String featureId1,
            @Parameter(description = "目标要素 ID") @RequestParam String featureId2) {
        try {
            return ApiResponse.ok(spatialService.distance(featureId1, featureId2));
        } catch (Exception e) {
            throw BizException.badRequest("距离计算失败: " + e.getMessage());
        }
    }

    @GetMapping("/feature/intersects")
    @Operation(summary = "判断两个要素是否相交")
    public ApiResponse<Boolean> intersects(
            @Parameter(description = "要素 ID 1") @RequestParam String featureId1,
            @Parameter(description = "要素 ID 2") @RequestParam String featureId2) {
        try {
            return ApiResponse.ok(spatialService.intersects(featureId1, featureId2));
        } catch (Exception e) {
            throw BizException.badRequest("空间关系判断失败: " + e.getMessage());
        }
    }

    @GetMapping("/layer/{layerId1}/intersection/{layerId2}")
    @Operation(summary = "计算两个图层的相交结果")
    public ApiResponse<List<Map<String, Object>>> intersection(
            @Parameter(description = "图层 ID 1") @PathVariable String layerId1,
            @Parameter(description = "图层 ID 2") @PathVariable String layerId2) {
        try {
            return ApiResponse.ok(spatialService.intersection(layerId1, layerId2));
        } catch (Exception e) {
            throw BizException.badRequest("叠加分析失败: " + e.getMessage());
        }
    }
}
