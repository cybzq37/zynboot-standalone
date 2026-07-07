package com.zynboot.map.service.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.map.infrastructure.entity.MapDataSource;
import com.zynboot.map.infrastructure.entity.MapLayerSource;
import com.zynboot.map.infrastructure.mapper.MapDataSourceMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 统一要素查询服务：按 source_type 解析单一查询主源，避免在服务层聚合多源结果后再分页。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureService {

    private final List<FeatureQueryHandler> handlers;
    private final MapLayerSourceMapper sourceMapper;
    private final MapDataSourceMapper dataSourceMapper;
    private final DynamicDataSourceService dynamicDataSourceService;

    public FeatureQueryResult list(String layerId, String sourceId, int limit, int offset) {
        ResolvedSource resolved = resolveSource(layerId, sourceId, Operation.LIST);
        FeatureQueryHandler handler = requireHandler(resolved.source().getType());
        return new FeatureQueryResult(
                resolved.source().getId(),
                resolved.source().getType(),
                handler.list(resolved.source().getId(), layerId, limit, offset),
                handler.count(resolved.source().getId(), layerId));
    }

    public FeatureQueryResult queryByBbox(String layerId, String sourceId, double[] bbox, int limit, int offset) {
        ResolvedSource resolved = resolveSource(layerId, sourceId, Operation.BBOX);
        FeatureQueryHandler handler = requireHandler(resolved.source().getType());
        return new FeatureQueryResult(
                resolved.source().getId(),
                resolved.source().getType(),
                handler.queryByBbox(resolved.source().getId(), layerId, bbox, limit, offset),
                handler.countByBbox(resolved.source().getId(), layerId, bbox));
    }

    public FeatureQueryResult search(String layerId, String sourceId, String query, int limit, int offset) {
        ResolvedSource resolved = resolveSource(layerId, sourceId, Operation.SEARCH);
        FeatureQueryHandler handler = requireHandler(resolved.source().getType());
        List<Map<String, Object>> items = handler.search(resolved.source().getId(), layerId, query, limit, offset);
        long total = "POSTGIS".equals(resolved.source().getType()) ? 0 : handler.count(resolved.source().getId(), layerId);
        return new FeatureQueryResult(
                resolved.source().getId(),
                resolved.source().getType(),
                items,
                total);
    }

    public long count(String layerId, String sourceId) {
        ResolvedSource resolved = resolveSource(layerId, sourceId, Operation.LIST);
        return requireHandler(resolved.source().getType()).count(resolved.source().getId(), layerId);
    }

    /**
     * 测试外部数据源连接。
     */
    public boolean testDataSource(String dataSourceId) {
        MapDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) return false;
        return dynamicDataSourceService.testConnection(ds);
    }

    private ResolvedSource resolveSource(String layerId, String sourceId, Operation operation) {
        // 约定：一个图层最多一个数据源，sourceId 参数已不再需要（保留仅为向后兼容）
        List<MapLayerSource> sources = sourceMapper.selectList(
                new LambdaQueryWrapper<MapLayerSource>()
                        .eq(MapLayerSource::getLayerId, layerId)
                        .eq(MapLayerSource::getStatus, "COMPLETED"));
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("图层没有可查询的数据源: " + layerId);
        }
        return new ResolvedSource(sources.get(0));
    }

    private FeatureQueryHandler requireHandler(String sourceType) {
        return handlers.stream()
                .filter(handler -> handler.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 sourceType 对应的查询处理器: " + sourceType));
    }

    private enum Operation {
        LIST, BBOX, SEARCH
    }

    private record ResolvedSource(MapLayerSource source) {
    }

    public record FeatureQueryResult(String sourceId,
                                     String sourceType,
                                     List<Map<String, Object>> items,
                                     long total) {
    }
}
