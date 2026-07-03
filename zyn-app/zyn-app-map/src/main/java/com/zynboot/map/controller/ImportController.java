package com.zynboot.map.controller;

import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.response.source.ImportRes;
import com.zynboot.map.service.ImportService;
import com.zynboot.map.service.LayerCacheVersionService;
import com.zynboot.map.service.MapSourceService;
import com.zynboot.map.service.MapTaskService;
import com.zynboot.map.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
@Tag(name = "数据导入", description = "导入矢量、栅格以及外部库表数据")
public class ImportController {

    private final ImportService importService;
    private final VersionService versionService;
    private final MapSourceService sourceService;
    private final MapTaskService taskService;
    private final LayerCacheVersionService layerCacheVersionService;

    @Value("${zyn.map.raster.auto-tile:true}")
    private boolean autoTile;

    @PostMapping("/import")
    @Operation(summary = "导入矢量文件", description = "上传矢量文件并挂接到指定图层，导入前会自动创建版本快照")
    public ApiResponse<ImportRes> importVector(
            @Parameter(description = "矢量数据文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "目标图层 ID") @RequestParam(required = false) String layerId,
            @Parameter(description = "源数据坐标系 EPSG 编码", example = "4326") @RequestParam String sourceSrid,
            @Parameter(description = "源名称，不传时取文件名") @RequestParam(required = false) String sourceName) {
        try {
            if (layerId == null || layerId.isBlank()) {
                throw BizException.badRequest("layerId 不能为空");
            }
            versionService.createSnapshot(layerId, "IMPORT", "导入前自动快照");
            SourceAggregate source = importService.importVector(file, layerId, sourceSrid, sourceName);
            layerCacheVersionService.bumpVersion(layerId);
            return ApiResponse.ok(ImportRes.builder()
                    .source(sourceService.toRes(source))
                    .task(null)
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Vector import failed", e);
            throw BizException.badRequest("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/import/raster")
    @Operation(summary = "导入栅格文件", description = "上传栅格文件并注册为图层数据源，可根据配置自动提交切片任务")
    public ApiResponse<ImportRes> importRaster(
            @Parameter(description = "栅格数据文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "目标图层 ID") @RequestParam String layerId,
            @Parameter(description = "源数据坐标系 EPSG 编码", example = "3857") @RequestParam String sourceSrid,
            @Parameter(description = "源名称，不传时取文件名") @RequestParam(required = false) String sourceName) {
        try {
            SourceAggregate source = importService.importRaster(file, layerId, sourceSrid,
                    sourceName != null ? sourceName : file.getOriginalFilename());
            return ApiResponse.ok(buildRasterImportRes(source));
        } catch (Exception e) {
            log.error("Raster import failed", e);
            throw BizException.badRequest("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/import/raster/register")
    @Operation(summary = "注册已有栅格文件")
    public ApiResponse<ImportRes> registerRaster(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "栅格注册参数", required = true)
            @Valid @RequestBody RasterRegisterCmd cmd) {
        try {
            SourceAggregate source = importService.registerRaster(
                    cmd.getFilePath(), cmd.getLayerId(), cmd.getSourceSrid(), cmd.getSourceName());
            return ApiResponse.ok(buildRasterImportRes(source));
        } catch (Exception e) {
            log.error("Raster register failed", e);
            throw BizException.badRequest("注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/import/postgis")
    @Operation(summary = "注册 PostGIS 表")
    public ApiResponse<ImportRes> registerPostgis(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PostGIS 注册参数", required = true)
            @Valid @RequestBody PostgisImportCmd cmd) {
        try {
            SourceAggregate source = importService.registerPostgis(
                    cmd.getLayerId(), cmd.getSourceName(), cmd.getDataSourceId(),
                    cmd.getExternalSchema(), cmd.getExternalTable(),
                    cmd.getExternalGeomCol(), cmd.getExternalIdCol(), cmd.getSourceSrid());
            layerCacheVersionService.bumpVersion(cmd.getLayerId());
            return ApiResponse.ok(ImportRes.builder()
                    .source(sourceService.toRes(source))
                    .task(null)
                    .build());
        } catch (Exception e) {
            log.error("PostGIS register failed", e);
            throw BizException.badRequest("注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/import/elasticsearch")
    @Operation(summary = "注册 Elasticsearch 索引")
    public ApiResponse<ImportRes> registerElasticsearch(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Elasticsearch 注册参数", required = true)
            @Valid @RequestBody EsImportCmd cmd) {
        try {
            SourceAggregate source = importService.registerElasticsearch(
                    cmd.getLayerId(), cmd.getSourceName(), cmd.getDataSourceId(),
                    cmd.getIndexName(), cmd.getGeomField(), cmd.getSourceSrid());
            layerCacheVersionService.bumpVersion(cmd.getLayerId());
            return ApiResponse.ok(ImportRes.builder()
                    .source(sourceService.toRes(source))
                    .task(null)
                    .build());
        } catch (Exception e) {
            log.error("Elasticsearch register failed", e);
            throw BizException.badRequest("注册失败: " + e.getMessage());
        }
    }

    private ImportRes buildRasterImportRes(SourceAggregate source) {
        return ImportRes.builder()
                .source(sourceService.toRes(source))
                .task(autoTile ? taskService.submitTileTask(source.getId()) : null)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "栅格文件注册参数")
    public static class RasterRegisterCmd {
        @Schema(description = "栅格文件绝对路径或相对路径", example = "D:/data/raster/dem.tif")
        @NotBlank String filePath;
        @Schema(description = "目标图层 ID", example = "3d74d7a5f9b04b43a9b07b0fdc30e6bf")
        @NotBlank String layerId;
        @Schema(description = "源数据坐标系 EPSG 编码", example = "3857")
        @NotBlank String sourceSrid;
        @Schema(description = "源名称", example = "DEM 高程数据")
        String sourceName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "PostGIS 图层注册参数")
    public static class PostgisImportCmd {
        @Schema(description = "目标图层 ID", example = "3d74d7a5f9b04b43a9b07b0fdc30e6bf")
        @NotBlank String layerId;
        @Schema(description = "源名称", example = "道路中心线")
        String sourceName;
        @Schema(description = "数据源 ID", example = "6f7d71de0c9148e4ac3a793ee39d19cb")
        @NotBlank String dataSourceId;
        @Schema(description = "外部 schema，默认取数据源默认 schema", example = "public")
        String externalSchema;
        @Schema(description = "外部表名", example = "road_centerline")
        @NotBlank String externalTable;
        @Schema(description = "几何字段名", example = "geom")
        String externalGeomCol;
        @Schema(description = "主键字段名", example = "id")
        String externalIdCol;
        @Schema(description = "源数据坐标系 EPSG 编码", example = "4490")
        @NotBlank String sourceSrid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Elasticsearch 图层注册参数")
    public static class EsImportCmd {
        @Schema(description = "目标图层 ID", example = "3d74d7a5f9b04b43a9b07b0fdc30e6bf")
        @NotBlank String layerId;
        @Schema(description = "源名称", example = "POI 索引")
        String sourceName;
        @Schema(description = "数据源 ID", example = "6f7d71de0c9148e4ac3a793ee39d19cb")
        @NotBlank String dataSourceId;
        @Schema(description = "索引名称", example = "poi_index")
        @NotBlank String indexName;
        @Schema(description = "几何字段名", example = "location")
        String geomField;
        @Schema(description = "源数据坐标系 EPSG 编码", example = "4326")
        @NotBlank String sourceSrid;
    }
}
