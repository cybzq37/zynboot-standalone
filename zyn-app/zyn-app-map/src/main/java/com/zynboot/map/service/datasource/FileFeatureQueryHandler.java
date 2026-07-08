package com.zynboot.map.service.datasource;

import com.zynboot.map.infrastructure.mapper.MapLayerFeatureMapper;
import com.zynboot.map.infrastructure.mapper.MapSpatialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 本地数据库图层查询：查 map_layer_feature 表（PostGIS）。
 */
@Component
@RequiredArgsConstructor
public class FileFeatureQueryHandler implements FeatureQueryHandler {

    private final MapLayerFeatureMapper featureMapper;
    private final MapSpatialMapper spatialMapper;

    @Override
    public boolean supports(String sourceType) {
        return "LOCAL".equals(sourceType);
    }

    @Override
    public List<Map<String, Object>> list(String sourceId, String layerId, int limit, int offset) {
        if (sourceId != null && !sourceId.isBlank()) {
            return spatialMapper.findAsGeoJsonBySource(layerId, sourceId, limit, offset);
        }
        return spatialMapper.findAsGeoJson(layerId, limit, offset);
    }

    @Override
    public List<Map<String, Object>> queryByBbox(String sourceId, String layerId,
                                                  double[] bbox, int limit, int offset) {
        if (sourceId != null && !sourceId.isBlank()) {
            return spatialMapper.findAsGeoJsonBySourceAndBbox(
                    layerId, sourceId,
                    String.valueOf(bbox[0]), String.valueOf(bbox[1]),
                    String.valueOf(bbox[2]), String.valueOf(bbox[3]),
                    limit, offset);
        }
        return spatialMapper.findAsGeoJsonByBbox(
                layerId,
                String.valueOf(bbox[0]), String.valueOf(bbox[1]),
                String.valueOf(bbox[2]), String.valueOf(bbox[3]),
                limit, offset);
    }

    @Override
    public List<Map<String, Object>> search(String sourceId, String layerId,
                                             String query, int limit, int offset) {
        return spatialMapper.searchBm25(layerId, query, limit, offset);
    }

    @Override
    public long count(String sourceId, String layerId) {
        return featureMapper.countByLayerId(layerId);
    }

    @Override
    public long countByBbox(String sourceId, String layerId, double[] bbox) {
        if (sourceId != null && !sourceId.isBlank()) {
            return spatialMapper.countByLayerSourceAndBbox(
                    layerId, sourceId,
                    String.valueOf(bbox[0]), String.valueOf(bbox[1]),
                    String.valueOf(bbox[2]), String.valueOf(bbox[3]));
        }
        return spatialMapper.countByLayerAndBbox(
                layerId,
                String.valueOf(bbox[0]), String.valueOf(bbox[1]),
                String.valueOf(bbox[2]), String.valueOf(bbox[3]));
    }
}
