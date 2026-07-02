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
}
