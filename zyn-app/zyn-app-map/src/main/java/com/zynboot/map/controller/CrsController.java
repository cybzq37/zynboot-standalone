package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.domain.enums.LayerType;
import com.zynboot.map.domain.enums.SourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zynboot.infra.web.version.ApiVersion;

import java.util.Arrays;
import java.util.List;

@RestController
@ApiVersion("1")
@RequestMapping("/map/crs")
public class CrsController {

    @GetMapping("/presets")
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
    public static class CrsPreset {
        int code;
        String name;
        String description;
    }
}
