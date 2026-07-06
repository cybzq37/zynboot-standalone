package com.zynboot.jitu.controller;

import com.zynboot.jitu.command.geo.GeoResolveCmd;
import com.zynboot.jitu.response.geo.GeoResolveRes;
import com.zynboot.jitu.service.GeoResolveService;
import com.zynboot.kit.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/openapi/lbs/v1/geo")
@Tag(name = "极兔 Geo", description = "地址解析与围栏匹配")
public class GeoController {

    private final GeoResolveService geoResolveService;

    @PostMapping("/resolve")
    @Operation(summary = "解析地址并匹配围栏")
    public ApiResponse<GeoResolveRes> resolve(@Valid @RequestBody GeoResolveCmd cmd) {
        return ApiResponse.ok(geoResolveService.resolve(cmd));
    }
}
