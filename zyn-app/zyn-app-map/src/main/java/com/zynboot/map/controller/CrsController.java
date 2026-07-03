package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/map/crs")
@Tag(name = "坐标系", description = "提供地图模块常用坐标系预设")
public class CrsController {

    @GetMapping("/presets")
    @Operation(summary = "获取坐标系预设列表")
    public ApiResponse<List<CrsPreset>> presets() {
        return ApiResponse.ok(List.of(
                new CrsPreset(4490, "CGCS2000", "国家大地坐标系"),
                new CrsPreset(4326, "WGS84", "全球坐标系"),
                new CrsPreset(3857, "Web Mercator", "Web 墨卡托投影"),
                new CrsPreset(4547, "CGCS2000/3-degree", "CGCS2000 3度带"),
                new CrsPreset(4526, "CGCS2000/6-degree", "CGCS2000 6度带"),
                new CrsPreset(32649, "UTM 49N", "通用横轴墨卡托 49N"),
                new CrsPreset(32650, "UTM 50N", "通用横轴墨卡托 50N")
        ));
    }

    @Value
    @AllArgsConstructor
    @Schema(description = "坐标系预设")
    public static class CrsPreset {
        @Schema(description = "EPSG 编码", example = "3857")
        int code;
        @Schema(description = "坐标系名称", example = "Web Mercator")
        String name;
        @Schema(description = "坐标系说明", example = "Web 墨卡托投影")
        String description;
    }
}
