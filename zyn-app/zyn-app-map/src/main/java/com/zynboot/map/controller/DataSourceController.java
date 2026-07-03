package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.datasource.DataSourceSaveCmd;
import com.zynboot.map.response.datasource.ConnectionTestRes;
import com.zynboot.map.response.datasource.DataSourceRes;
import com.zynboot.map.service.MapDataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/datasource")
@Tag(name = "数据源管理", description = "管理 PostGIS、Elasticsearch 等外部数据源连接")
public class DataSourceController {

    private final MapDataSourceService dataSourceService;

    @GetMapping
    @Operation(summary = "查询数据源列表")
    public ApiResponse<List<DataSourceRes>> list() {
        return ApiResponse.ok(dataSourceService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取数据源详情")
    public ApiResponse<DataSourceRes> getById(@Parameter(description = "数据源 ID") @PathVariable String id) {
        return ApiResponse.ok(dataSourceService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建数据源")
    public ApiResponse<DataSourceRes> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "数据源创建参数", required = true)
            @Valid @RequestBody DataSourceSaveCmd cmd) {
        return ApiResponse.ok(dataSourceService.create(cmd));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新数据源")
    public ApiResponse<DataSourceRes> update(
            @Parameter(description = "数据源 ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "数据源更新参数", required = true)
            @Valid @RequestBody DataSourceSaveCmd cmd) {
        return ApiResponse.ok(dataSourceService.update(id, cmd));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据源")
    public ApiResponse<Void> delete(@Parameter(description = "数据源 ID") @PathVariable String id) {
        dataSourceService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试已有数据源连接")
    public ApiResponse<ConnectionTestRes> testConnection(@Parameter(description = "数据源 ID") @PathVariable String id) {
        return ApiResponse.ok(dataSourceService.testExisting(id));
    }

    @PostMapping("/test")
    @Operation(summary = "测试新数据源连接")
    public ApiResponse<ConnectionTestRes> testNewConnection(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "待测试的数据源参数", required = true)
            @RequestBody DataSourceSaveCmd cmd) {
        return ApiResponse.ok(dataSourceService.testNew(cmd));
    }
}
