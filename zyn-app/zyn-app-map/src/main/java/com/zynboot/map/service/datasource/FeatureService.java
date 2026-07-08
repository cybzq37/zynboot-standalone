package com.zynboot.map.service.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
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
        List<MapLayerSource> sources = sourceMapper.selectList(
                new LambdaQueryWrapper<MapLayerSource>()
                        .eq(MapLayerSource::getLayerId, layerId));
        if (sources.isEmpty()) {
            // 无数据源绑定的图层视为本地数据库图层（LOCAL 类型路由），
            // 要素存储在 map_layer_feature 表（PostGIS），由 FileFeatureQueryHandler 查询
            MapLayerSource localSource = new MapLayerSource();
            localSource.setLayerId(layerId);
            localSource.setType("LOCAL");
            return new ResolvedSource(localSource);
        }
        // 有数据源绑定，取已就绪的
        MapLayerSource completed = sources.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> {
                    MapLayerSource first = sources.get(0);
                    return BizException.badRequest(
                            "图层数据源未就绪，当前状态: " + first.getStatus() + "，请稍后重试或检查数据源配置");
                });
        return new ResolvedSource(completed);
    }

    private FeatureQueryHandler requireHandler(String sourceType) {
        return handlers.stream()
                .filter(handler -> handler.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> BizException.badRequest("不支持的数据源类型: " + sourceType));
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
