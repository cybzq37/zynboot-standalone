package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.task.TaskRes;
import com.zynboot.map.service.MapTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/task")
@Tag(name = "任务管理", description = "查询和控制地图模块后台任务")
public class TaskController {

    private final MapTaskService taskService;

    @GetMapping
    @Operation(summary = "查询后台任务列表")
    public ApiResponse<List<TaskRes>> list(
            @Parameter(description = "任务类型，例如 TILE") @RequestParam(required = false) String type,
            @Parameter(description = "任务状态，例如 PENDING、RUNNING、SUCCESS、FAILED") @RequestParam(required = false) String status) {
        return ApiResponse.ok(taskService.list(type, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public ApiResponse<TaskRes> getById(@Parameter(description = "任务 ID") @PathVariable String id) {
        return ApiResponse.ok(taskService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消任务")
    public ApiResponse<Void> cancel(@Parameter(description = "任务 ID") @PathVariable String id) {
        taskService.cancel(id);
        return ApiResponse.ok(null);
    }
}
