package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.source.ProxyHealthRes;
import com.zynboot.map.service.MapQueryFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/source")
@Tag(name = "代理健康检查", description = "查看或触发代理型数据源的健康检查")
public class ProxyHealthController {

    private final MapQueryFacadeService queryFacadeService;

    @GetMapping("/{id}/proxy/health")
    @Operation(summary = "获取代理源健康状态")
    public ApiResponse<ProxyHealthRes> getHealth(@Parameter(description = "源 ID") @PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.getProxyHealth(id));
    }

    @PostMapping("/{id}/proxy/check")
    @Operation(summary = "触发代理源健康检查")
    public ApiResponse<ProxyHealthRes> triggerCheck(@Parameter(description = "源 ID") @PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.triggerProxyCheck(id));
    }
}
