package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.command.datasource.DataSourceSaveCmd;
import com.zynboot.map.response.datasource.ConnectionTestRes;
import com.zynboot.map.response.datasource.DataSourceRes;
import com.zynboot.map.service.MapDataSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/datasource")
public class DataSourceController {

    private final MapDataSourceService dataSourceService;

    @GetMapping
    public ApiResponse<List<DataSourceRes>> list() {
        return ApiResponse.ok(dataSourceService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<DataSourceRes> getById(@PathVariable String id) {
        return ApiResponse.ok(dataSourceService.getById(id));
    }

    @PostMapping
    public ApiResponse<DataSourceRes> create(@Valid @RequestBody DataSourceSaveCmd cmd) {
        return ApiResponse.ok(dataSourceService.create(cmd));
    }

    @PutMapping("/{id}")
    public ApiResponse<DataSourceRes> update(@PathVariable String id, @Valid @RequestBody DataSourceSaveCmd cmd) {
        return ApiResponse.ok(dataSourceService.update(id, cmd));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        dataSourceService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<ConnectionTestRes> testConnection(@PathVariable String id) {
        return ApiResponse.ok(dataSourceService.testExisting(id));
    }

    @PostMapping("/test")
    public ApiResponse<ConnectionTestRes> testNewConnection(@RequestBody DataSourceSaveCmd cmd) {
        return ApiResponse.ok(dataSourceService.testNew(cmd));
    }
}
