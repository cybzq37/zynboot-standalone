package com.zynboot.map.service;

import com.zynboot.infra.storage.model.UploadedFileInfo;
import com.zynboot.infra.storage.service.StorageService;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapSourceRaster;
import com.zynboot.map.infrastructure.importer.VectorImporter;
import com.zynboot.map.infrastructure.mapper.MapLayerFeatureMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerFieldMapper;
import com.zynboot.map.infrastructure.mapper.MapSourceRasterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final LayerRepository layerRepository;
    private final SourceRepository sourceRepository;
    private final StorageService storageService;
    private final MapSourceRasterMapper rasterMapper;
    private final MapLayerFieldMapper fieldMapper;
    private final MapLayerFeatureMapper featureMapper;
    private final List<VectorImporter> importers;

    @Value("${zyn.map.raster.root-path:./map-data/raster}")
    private String rasterRootPath;

    // ── 矢量导入（PostGIS Geometry 写入）────────────────────

    @Transactional
    public SourceAggregate importVector(MultipartFile file, String layerId,
                                         String sourceSrid, String sourceName) throws Exception {
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));

        String originalName = file.getOriginalFilename();
        String format = detectFormat(originalName);

        VectorImporter importer = importers.stream()
                .filter(i -> i.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的格式: " + format));

        List<VectorImporter.FeatureRecord> features = importer.parse(file.getInputStream(), sourceSrid);
        UploadedFileInfo uploaded = storageService.upload(file);

        SourceAggregate source = SourceAggregate.create(layerId,
                sourceName != null ? sourceName : originalName, "LOCAL", format);
        source.getEntity().setSourceSrid(parseSrid(sourceSrid));
        source.getEntity().setTargetSrid(layer.getTargetSrid());
        source.getEntity().setStorageKey(uploaded.getKey());
        sourceRepository.save(source);

        int imported = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < features.size(); i++) {
            VectorImporter.FeatureRecord rec = features.get(i);
            try {
                // 使用 PostGIS 函数写入几何：ST_GeogFromGeoJSON（统一存 geography/4326）
                String geomJson = rec.geometryGeoJson();
                String propsJson = rec.propertiesJson();

                // 通过 Mapper 的自定义 SQL 写入（带 PostGIS 函数）
                featureMapper.insertWithGeometry(
                        IdUtils.snowflakeId(),
                        layerId,
                        source.getId(),
                        propsJson,
                        geomJson
                );
                imported++;
            } catch (Exception e) {
                errors.add("feature[" + i + "]: " + e.getMessage());
                if (errors.size() >= 100) break;
            }
        }

        source.markCompleted(imported);
        source.getEntity().setMessage(errors.isEmpty() ? null : String.join("\n", errors));
        sourceRepository.update(source);

        layer.incrementFeatureCount(imported);
        layer.incrementSourceCount();
        layerRepository.update(layer);

        // 失效 MVT 缓存（如果有 MvtService）
        // mvtService.invalidateLayerCache(layerId);  // 由 TileController 层处理

        log.info("Vector imported: sourceId={}, format={}, imported={}/{}", source.getId(), format, imported, features.size());
        return source;
    }

    // ── 栅格导入 ───────────────────────────────────────────

    @Transactional
    public SourceAggregate importRaster(MultipartFile file, String layerId,
                                         String sourceSrid, String sourceName) throws IOException {
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));

        UploadedFileInfo uploaded = storageService.upload(file);

        SourceAggregate source = SourceAggregate.create(layerId, sourceName, "FILE", "GEOTIFF");
        source.getEntity().setSourceSrid(parseSrid(sourceSrid));
        source.getEntity().setTargetSrid(layer.getTargetSrid());
        source.getEntity().setStorageKey(uploaded.getKey());
        source.markCompleted(0);
        sourceRepository.save(source);

        MapSourceRaster raster = new MapSourceRaster();
        raster.setSourceId(source.getId());
        raster.setPath(uploaded.getKey());
        raster.setCompressedBytes(file.getSize());
        rasterMapper.insert(raster);

        layer.incrementSourceCount();
        layerRepository.update(layer);

        log.info("Raster imported: sourceId={}, file={}", source.getId(), file.getOriginalFilename());
        return source;
    }

    @Transactional
    public SourceAggregate registerRaster(String filePath, String layerId,
                                           String sourceSrid, String sourceName) throws IOException {
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));

        Path path = Paths.get(filePath).normalize();
        Path rootPath = Paths.get(rasterRootPath).normalize();
        if (!Files.exists(path)) throw new IllegalArgumentException("文件不存在: " + filePath);
        if (!path.startsWith(rootPath)) throw new IllegalArgumentException("文件路径不在允许的栅格根目录内: " + rasterRootPath);

        SourceAggregate source = SourceAggregate.create(layerId, sourceName, "FILE", "GEOTIFF");
        source.getEntity().setSourceSrid(parseSrid(sourceSrid));
        source.getEntity().setTargetSrid(layer.getTargetSrid());
        source.getEntity().setStorageKey(filePath);
        source.markCompleted(0);
        sourceRepository.save(source);

        MapSourceRaster raster = new MapSourceRaster();
        raster.setSourceId(source.getId());
        raster.setPath(filePath);
        raster.setCompressedBytes(Files.size(path));
        rasterMapper.insert(raster);

        layer.incrementSourceCount();
        layerRepository.update(layer);

        log.info("Raster registered: sourceId={}, path={}", source.getId(), filePath);
        return source;
    }

    // ── PostGIS 直查注册 ─────────────────────────────────

    @Transactional
    public SourceAggregate registerPostgis(String layerId, String sourceName,
                                            String dataSourceId, String externalSchema,
                                            String externalTable, String externalGeomCol,
                                            String externalIdCol, String sourceSrid) {
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));

        SourceAggregate source = SourceAggregate.create(layerId, sourceName, "POSTGIS", null);
        source.getEntity().setSourceSrid(parseSrid(sourceSrid));
        source.getEntity().setTargetSrid(layer.getTargetSrid());
        source.getEntity().setDataSourceId(dataSourceId);
        source.getEntity().setExternalSchema(externalSchema);
        source.getEntity().setExternalTable(externalTable);
        source.getEntity().setExternalGeomCol(externalGeomCol != null ? externalGeomCol : "geom");
        source.getEntity().setExternalIdCol(externalIdCol != null ? externalIdCol : "gid");
        source.markCompleted(0);
        sourceRepository.save(source);

        layer.incrementSourceCount();
        layerRepository.update(layer);

        log.info("PostGIS source registered: sourceId={}, table={}.{}", source.getId(), externalSchema, externalTable);
        return source;
    }

    // ── Elasticsearch 注册 ───────────────────────────────

    @Transactional
    public SourceAggregate registerElasticsearch(String layerId, String sourceName,
                                                  String dataSourceId, String indexName,
                                                  String geomField, String sourceSrid) {
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));

        SourceAggregate source = SourceAggregate.create(layerId, sourceName, "ELASTICSEARCH", null);
        source.getEntity().setSourceSrid(parseSrid(sourceSrid));
        source.getEntity().setTargetSrid(layer.getTargetSrid());
        source.getEntity().setDataSourceId(dataSourceId);
        source.getEntity().setExternalTable(indexName);        // ES 索引名
        source.getEntity().setExternalGeomCol(geomField != null ? geomField : "location");
        source.markCompleted(0);
        sourceRepository.save(source);

        layer.incrementSourceCount();
        layerRepository.update(layer);

        log.info("ES source registered: sourceId={}, index={}", source.getId(), indexName);
        return source;
    }

    // ── 内部方法 ───────────────────────────────────────────

    private String detectFormat(String filename) {
        if (filename == null) return "UNKNOWN";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) return "CSV";
        if (lower.endsWith(".geojson") || lower.endsWith(".json")) return "GEOJSON";
        if (lower.endsWith(".shp")) return "SHP";
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) return "GEOTIFF";
        return "UNKNOWN";
    }

    private Integer parseSrid(String srid) {
        if (srid == null) return null;
        try {
            return Integer.parseInt(srid.replace("EPSG:", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
