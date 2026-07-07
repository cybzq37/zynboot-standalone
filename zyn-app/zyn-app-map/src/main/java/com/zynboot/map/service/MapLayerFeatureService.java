package com.zynboot.map.service;

import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.command.feature.FeatureSaveCmd;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.infrastructure.entity.MapLayerFeature;
import com.zynboot.map.infrastructure.mapper.MapLayerFeatureMapper;
import com.zynboot.map.infrastructure.mapper.MapSpatialMapper;
import com.zynboot.map.response.feature.FeaturePageRes;
import com.zynboot.map.response.feature.FeatureRes;
import com.zynboot.map.service.datasource.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MapLayerFeatureService {

    private final MapLayerFeatureMapper featureMapper;
    private final MapSpatialMapper spatialMapper;
    private final FeatureService queryService;
    private final LayerRepository layerRepository;
    private final LayerCacheVersionService layerCacheVersionService;

    /** 单次最多返回的要素数量上限 */
    private static final int MAX_FEATURE_LIMIT = 1000;

    public FeaturePageRes listByLayer(String layerId, String sourceId, String bbox) {
        int limit = MAX_FEATURE_LIMIT;
        int offset = 0;
        FeatureService.FeatureQueryResult result;
        if (bbox != null && !bbox.isBlank()) {
            result = queryService.queryByBbox(layerId, sourceId, parseBbox(bbox), limit, offset);
        } else {
            result = queryService.list(layerId, sourceId, limit, offset);
        }
        return FeaturePageRes.builder()
                .items(result.items())
                .total(result.total())
                .pageNum(1)
                .pageSize(limit)
                .querySourceId(result.sourceId())
                .querySourceType(result.sourceType())
                .build();
    }

    public FeaturePageRes search(String layerId, String sourceId, String query, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        FeatureService.FeatureQueryResult result = queryService.search(layerId, sourceId, query, pageSize, offset);
        return FeaturePageRes.builder()
                .items(result.items())
                .total(result.total())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .querySourceId(result.sourceId())
                .querySourceType(result.sourceType())
                .build();
    }

    public FeatureRes getById(Long id) {
        MapLayerFeature feature = featureMapper.selectViewById(id);
        if (feature == null) {
            throw BizException.notFound("要素");
        }
        return toRes(feature);
    }

    public List<Map<String, Object>> listAsGeoJson(String layerId, String sourceId, int pageNum, int pageSize) {
        requireLayer(layerId);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        if (sourceId != null && !sourceId.isBlank()) {
            return spatialMapper.findAsGeoJsonBySource(layerId, sourceId, pageSize, offset);
        }
        return spatialMapper.findAsGeoJson(layerId, pageSize, offset);
    }

    public List<Map<String, Object>> cluster(String layerId, int k, String bbox) {
        requireLayer(layerId);
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = parseBboxParts(bbox);
            return spatialMapper.clusterWithBbox(layerId, k, parts[0], parts[1], parts[2], parts[3]);
        }
        return spatialMapper.cluster(layerId, k);
    }

    public List<Map<String, Object>> searchBm25(String layerId, String query, String bbox, int pageNum, int pageSize) {
        requireLayer(layerId);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = parseBboxParts(bbox);
            return spatialMapper.searchBm25WithBbox(layerId, query, parts[0], parts[1], parts[2], parts[3], pageSize, offset);
        }
        return spatialMapper.searchBm25(layerId, query, pageSize, offset);
    }

    @Transactional
    public FeatureRes create(String layerId, FeatureSaveCmd cmd) {
        LayerAggregate layer = requireLayer(layerId);
        long id = IdUtils.snowflakeId();
        featureMapper.insertWithGeometry(
                id,
                layerId,
                cmd.getSourceId(),
                cmd.getProperties(),
                cmd.getGeometry(),
                String.valueOf(layer.getTargetSrid()));
        layer.incrementFeatureCount(1);
        layerRepository.update(layer);
        layerCacheVersionService.bumpVersion(layerId);
        return getById(id);
    }

    @Transactional
    public FeatureRes update(Long id, FeatureSaveCmd cmd) {
        MapLayerFeature existing = featureMapper.selectViewById(id);
        if (existing == null) {
            throw BizException.notFound("要素");
        }
        LayerAggregate layer = requireLayer(existing.getLayerId());
        String resolvedSourceId = cmd.getSourceId() != null && !cmd.getSourceId().isBlank()
                ? cmd.getSourceId()
                : existing.getSourceId();
        featureMapper.updateWithGeometry(
                id,
                resolvedSourceId,
                cmd.getProperties(),
                cmd.getGeometry(),
                String.valueOf(layer.getTargetSrid()));
        layerCacheVersionService.bumpVersion(existing.getLayerId());
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        MapLayerFeature existing = featureMapper.selectViewById(id);
        if (existing == null) {
            return;
        }
        featureMapper.deleteByIdValue(id);
        layerRepository.findById(existing.getLayerId()).ifPresent(layer -> {
            int current = layer.getFeatureCount() != null ? layer.getFeatureCount() : 0;
            layer.getEntity().setFeatureCount(Math.max(0, current - 1));
            layerRepository.update(layer);
        });
        layerCacheVersionService.bumpVersion(existing.getLayerId());
    }

    private LayerAggregate requireLayer(String layerId) {
        return layerRepository.findById(layerId)
                .orElseThrow(() -> BizException.notFound("图层"));
    }

    private double[] parseBbox(String bbox) {
        String[] parts = parseBboxParts(bbox);
        return new double[] {
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim())
        };
    }

    private String[] parseBboxParts(String bbox) {
        String[] parts = bbox.split(",");
        if (parts.length != 4) {
            throw BizException.badRequest("bbox 格式应为 minX,minY,maxX,maxY");
        }
        return parts;
    }

    private FeatureRes toRes(MapLayerFeature feature) {
        return FeatureRes.builder()
                .id(feature.getId())
                .layerId(feature.getLayerId())
                .sourceId(feature.getSourceId())
                .properties(feature.getProperties())
                .geometry(feature.getGeometry())
                .build();
    }
}
