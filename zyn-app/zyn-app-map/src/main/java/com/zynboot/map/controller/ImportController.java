package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.response.source.ImportRes;
import com.zynboot.map.service.ImportService;
import com.zynboot.map.service.LayerCacheVersionService;
import com.zynboot.map.service.MapSourceService;
import com.zynboot.map.service.MapTaskService;
import com.zynboot.map.service.VersionService;
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
@ApiVersion("1")
@RequestMapping("/map")
public class ImportController {

    private final ImportService importService;
    private final VersionService versionService;
    private final MapSourceService sourceService;
    private final MapTaskService taskService;
    private final LayerCacheVersionService layerCacheVersionService;

    @Value("${zyn.map.raster.auto-tile:true}")
    private boolean autoTile;

    @PostMapping("/import")
    public ApiResponse<ImportRes> importVector(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String layerId,
            @RequestParam String sourceSrid,
            @RequestParam(required = false) String sourceName) {
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
    public ApiResponse<ImportRes> importRaster(
            @RequestParam("file") MultipartFile file,
            @RequestParam String layerId,
            @RequestParam String sourceSrid,
            @RequestParam(required = false) String sourceName) {
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
    public ApiResponse<ImportRes> registerRaster(@Valid @RequestBody RasterRegisterCmd cmd) {
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
    public ApiResponse<ImportRes> registerPostgis(@Valid @RequestBody PostgisImportCmd cmd) {
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
    public ApiResponse<ImportRes> registerElasticsearch(@Valid @RequestBody EsImportCmd cmd) {
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
    public static class RasterRegisterCmd {
        @NotBlank String filePath;
        @NotBlank String layerId;
        @NotBlank String sourceSrid;
        String sourceName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostgisImportCmd {
        @NotBlank String layerId;
        String sourceName;
        @NotBlank String dataSourceId;
        String externalSchema;
        @NotBlank String externalTable;
        String externalGeomCol;
        String externalIdCol;
        @NotBlank String sourceSrid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EsImportCmd {
        @NotBlank String layerId;
        String sourceName;
        @NotBlank String dataSourceId;
        @NotBlank String indexName;
        String geomField;
        @NotBlank String sourceSrid;
    }
}
