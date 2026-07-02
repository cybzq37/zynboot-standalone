package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.source.ProxyHealthRes;
import com.zynboot.map.service.MapQueryFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/source")
public class ProxyHealthController {

    private final MapQueryFacadeService queryFacadeService;

    @GetMapping("/{id}/proxy/health")
    public ApiResponse<ProxyHealthRes> getHealth(@PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.getProxyHealth(id));
    }

    @PostMapping("/{id}/proxy/check")
    public ApiResponse<ProxyHealthRes> triggerCheck(@PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.triggerProxyCheck(id));
    }
}
