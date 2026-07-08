package com.zynboot.map.service.datasource;

import java.util.List;
import java.util.Map;

/**
 * 统一要素查询接口（按 source_type 路由到不同存储引擎）。
 */
public interface FeatureQueryHandler {

    /**
     * 是否支持该 source 类型。
     */
    boolean supports(String sourceType);

    /**
     * 列表查询。
     */
    List<Map<String, Object>> list(String sourceId, String layerId, int limit, int offset);

    /**
     * 空间查询（bbox 范围内）。
     *
     * @param sourceId 数据源 ID
     * @param layerId  图层 ID
     * @param bbox     [minX, minY, maxX, maxY]
     * @param limit    最大返回数
     * @param offset   偏移
     * @return 要素列表（每个 Map 包含 id, properties, geometry 等）
     */
    List<Map<String, Object>> queryByBbox(String sourceId, String layerId,
                                           double[] bbox, int limit, int offset);

    /**
     * 全文搜索（BM25）。
     */
    List<Map<String, Object>> search(String sourceId, String layerId,
                                      String query, int limit, int offset);

    /**
     * 要素总数。
     */
    long count(String sourceId, String layerId);

    /**
     * 要素总数（bbox 内）。
     */
    long countByBbox(String sourceId, String layerId, double[] bbox);

    /**
     * 带 filter + sort 的查询。
     *
     * @param query   筛选/排序条件，可为 null
     * @param bbox    空间范围，可为 null
     */
    default List<Map<String, Object>> queryWithFilter(String sourceId, String layerId,
                                                       FeatureQuery query, double[] bbox,
                                                       int limit, int offset) {
        if (bbox != null) {
            return queryByBbox(sourceId, layerId, bbox, limit, offset);
        }
        return list(sourceId, layerId, limit, offset);
    }

    /**
     * 带 filter + sort 的总数。
     */
    default long countWithFilter(String sourceId, String layerId,
                                  FeatureQuery query, double[] bbox) {
        if (bbox != null) {
            return countByBbox(sourceId, layerId, bbox);
        }
        return count(sourceId, layerId);
    }
}
