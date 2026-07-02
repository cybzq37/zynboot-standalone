package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.task.TaskRes;
import com.zynboot.map.service.MapTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/task")
public class TaskController {

    private final MapTaskService taskService;

    @GetMapping
    public ApiResponse<List<TaskRes>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(taskService.list(type, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskRes> getById(@PathVariable String id) {
        return ApiResponse.ok(taskService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String id) {
        taskService.cancel(id);
        return ApiResponse.ok(null);
    }
}
