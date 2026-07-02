package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.audit.AuditLogRes;
import com.zynboot.map.service.MapQueryFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/audit")
public class AuditController {

    private final MapQueryFacadeService queryFacadeService;

    @GetMapping
    public ApiResponse<List<AuditLogRes>> list(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(queryFacadeService.listAudit(targetType, targetId, action, operatorId, pageNum, pageSize));
    }

    @GetMapping("/{targetType}/{targetId}")
    public ApiResponse<List<AuditLogRes>> getByTarget(@PathVariable String targetType, @PathVariable String targetId) {
        return ApiResponse.ok(queryFacadeService.getAuditByTarget(targetType, targetId));
    }
}
