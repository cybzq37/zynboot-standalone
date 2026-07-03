package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.audit.AuditLogRes;
import com.zynboot.map.service.MapQueryFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/audit")
@Tag(name = "审计日志", description = "查询地图模块的审计与操作日志")
public class AuditController {

    private final MapQueryFacadeService queryFacadeService;

    @GetMapping
    @Operation(summary = "按条件查询审计日志")
    public ApiResponse<List<AuditLogRes>> list(
            @Parameter(description = "目标类型，例如 LAYER、SOURCE、INSTANCE") @RequestParam(required = false) String targetType,
            @Parameter(description = "目标 ID") @RequestParam(required = false) String targetId,
            @Parameter(description = "操作类型，例如 CREATE、UPDATE、DELETE") @RequestParam(required = false) String action,
            @Parameter(description = "操作人 ID") @RequestParam(required = false) String operatorId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(queryFacadeService.listAudit(targetType, targetId, action, operatorId, pageNum, pageSize));
    }

    @GetMapping("/{targetType}/{targetId}")
    @Operation(summary = "查询指定目标的审计日志")
    public ApiResponse<List<AuditLogRes>> getByTarget(
            @Parameter(description = "目标类型") @PathVariable String targetType,
            @Parameter(description = "目标 ID") @PathVariable String targetId) {
        return ApiResponse.ok(queryFacadeService.getAuditByTarget(targetType, targetId));
    }
}
