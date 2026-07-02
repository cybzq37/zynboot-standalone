package com.zynboot.map.service.datasource;

import com.zynboot.infra.es.EsClient;
import com.zynboot.map.infrastructure.entity.MapLayerSource;
import com.zynboot.map.infrastructure.mapper.MapDataSourceMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Elasticsearch 查询：通过 EsClient（zyn-infra-es）查询。
 * 复用 EsClient + ElasticsearchOperations，支持集群多节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsFeatureQueryHandler implements FeatureQueryHandler {

    private final MapLayerSourceMapper sourceMapper;
    private final EsClientManager esClientManager;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean supports(String sourceType) {
        return "ELASTICSEARCH".equals(sourceType);
    }

    @Override
    public List<Map<String, Object>> list(String sourceId, String layerId, int limit, int offset) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return Collections.emptyList();
        return executeSearch(source.getDataSourceId(), "{\"match_all\": {}}", limit, offset);
    }

    @Override
    public List<Map<String, Object>> queryByBbox(String sourceId, String layerId,
                                                  double[] bbox, int limit, int offset) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return Collections.emptyList();

        String geomField = source.getExternalGeomCol() != null ? source.getExternalGeomCol() : "location";

        String jsonQuery = """
                {
                  "bool": {
                    "filter": {
                      "geo_bounding_box": {
                        "%s": {
                          "top_left": { "lat": %f, "lon": %f },
                          "bottom_right": { "lat": %f, "lon": %f }
                        }
                      }
                    }
                  }
                }
                """.formatted(geomField, bbox[3], bbox[0], bbox[1], bbox[2]);

        return executeSearch(source.getDataSourceId(), jsonQuery, limit, offset);
    }

    @Override
    public List<Map<String, Object>> search(String sourceId, String layerId,
                                             String queryStr, int limit, int offset) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return Collections.emptyList();

        String jsonQuery = """
                {
                  "multi_match": {
                    "query": "%s",
                    "fields": ["*"],
                    "type": "best_fields"
                  }
                }
                """.formatted(escapeJson(queryStr));

        return executeSearch(source.getDataSourceId(), jsonQuery, limit, offset);
    }

    @Override
    public long count(String sourceId, String layerId) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return 0;

        try {
            EsClient esClient = esClientManager.getOrCreateEsClient(source.getDataSourceId());
            Query query = new StringQuery("{\"match_all\": {}}");
            return esClient.getOperations().count(query, Object.class);
        } catch (Exception e) {
            log.error("ES count failed: sourceId={}", sourceId, e);
            return 0;
        }
    }

    @Override
    public long countByBbox(String sourceId, String layerId, double[] bbox) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return 0;

        String geomField = source.getExternalGeomCol() != null ? source.getExternalGeomCol() : "location";
        String jsonQuery = """
                {
                  "bool": {
                    "filter": {
                      "geo_bounding_box": {
                        "%s": {
                          "top_left": { "lat": %f, "lon": %f },
                          "bottom_right": { "lat": %f, "lon": %f }
                        }
                      }
                    }
                  }
                }
                """.formatted(geomField, bbox[3], bbox[0], bbox[1], bbox[2]);
        return executeCount(source.getDataSourceId(), jsonQuery);
    }

    private List<Map<String, Object>> executeSearch(String dataSourceId, String jsonQuery, int limit, int offset) {
        try {
            EsClient esClient = esClientManager.getOrCreateEsClient(dataSourceId);

            Query query = new StringQuery(jsonQuery,
                    org.springframework.data.domain.PageRequest.of(
                            offset / Math.max(limit, 1), limit));

            SearchHits<?> hits = esClient.getOperations().search(query, Object.class);
            List<Map<String, Object>> results = new ArrayList<>();
            for (SearchHit<?> hit : hits) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("id", hit.getId());
                if (hit.getContent() instanceof Map<?, ?> sourceMap) {
                    doc.putAll((Map<String, Object>) sourceMap);
                } else {
                    doc.put("data", hit.getContent());
                }
                results.add(doc);
            }
            return results;
        } catch (Exception e) {
            log.error("ES search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private long executeCount(String dataSourceId, String jsonQuery) {
        try {
            EsClient esClient = esClientManager.getOrCreateEsClient(dataSourceId);
            Query query = new StringQuery(jsonQuery);
            return esClient.getOperations().count(query, Object.class);
        } catch (Exception e) {
            log.error("ES count failed: {}", e.getMessage());
            return 0;
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
