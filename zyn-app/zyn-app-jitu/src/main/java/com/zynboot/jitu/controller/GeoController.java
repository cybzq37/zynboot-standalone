package com.zynboot.jitu.controller;

import com.zynboot.jitu.response.geo.GeoResolveRes;
import com.zynboot.jitu.service.GeoResolveService;
import com.zynboot.kit.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/openapi/seg4code/v1/geo")
@Tag(name = "极兔 Geo", description = "地址解析与围栏匹配")
@Validated
public class GeoController {

    private final GeoResolveService geoResolveService;

    @GetMapping("/resolve")
    @Operation(summary = "解析地址并匹配围栏")
    public ApiResponse<GeoResolveRes> resolve(
            @Parameter(description = "1=取件，2=寄件", required = true)
            @RequestParam @NotNull @Min(1) @Max(2) Integer type,
            @Parameter(description = "结构化取件/收件地址", required = true)
            @RequestParam @NotBlank String address) {
        return ApiResponse.ok(geoResolveService.resolve(type, address));
    }
}
