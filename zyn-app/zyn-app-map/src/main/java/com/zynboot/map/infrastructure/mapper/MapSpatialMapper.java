package com.zynboot.map.infrastructure.mapper;

import com.zynboot.map.infrastructure.entity.MapLayerFeature;
import com.zynboot.map.service.datasource.FeatureQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MapSpatialMapper {

    // ── 空间分析（geography 类型，距离/面积/缓冲区单位为米） ─────

    @Select("SELECT ST_AsGeoJSON(ST_Buffer(geometry, #{distanceMeters})::geometry) FROM map_layer_feature WHERE id = CAST(#{featureId} AS BIGINT)")
    String buffer(@Param("featureId") String featureId, @Param("distanceMeters") double distanceMeters);

    @Select("SELECT ST_Distance(a.geometry, b.geometry) FROM map_layer_feature a, map_layer_feature b WHERE a.id = CAST(#{id1} AS BIGINT) AND b.id = CAST(#{id2} AS BIGINT)")
    double distance(@Param("id1") String featureId1, @Param("id2") String featureId2);

    @Select("SELECT ST_Area(geometry) FROM map_layer_feature WHERE id = CAST(#{featureId} AS BIGINT)")
    double area(@Param("featureId") String featureId);

    @Select("SELECT ST_Intersects(a.geometry, b.geometry) FROM map_layer_feature a, map_layer_feature b WHERE a.id = CAST(#{id1} AS BIGINT) AND b.id = CAST(#{id2} AS BIGINT)")
    boolean intersects(@Param("id1") String featureId1, @Param("id2") String featureId2);

    @Select("SELECT f.id, f.properties, ST_AsGeoJSON(f.geometry) as geometry FROM map_layer_feature f WHERE f.layer_id = #{layerId1} AND EXISTS (SELECT 1 FROM map_layer_feature g WHERE g.layer_id = #{layerId2} AND ST_Intersects(f.geometry, g.geometry))")
    List<Map<String, Object>> intersection(@Param("layerId1") String layerId1, @Param("layerId2") String layerId2);

    // ── 空间查询（geography 列与 geometry envelope 比较需转 geometry） ──

    @Select("SELECT * FROM map_layer_feature WHERE layer_id = #{layerId} AND geometry::geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326) LIMIT #{limit} OFFSET #{offset}")
    List<MapLayerFeature> findByBbox(@Param("layerId") String layerId, @Param("minX") String minX, @Param("minY") String minY, @Param("maxX") String maxX, @Param("maxY") String maxY, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry FROM map_layer_feature WHERE layer_id = #{layerId} LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJson(@Param("layerId") String layerId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry " +
            "FROM map_layer_feature WHERE layer_id = #{layerId} AND source_id = #{sourceId} LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJsonBySource(@Param("layerId") String layerId,
                                                    @Param("sourceId") String sourceId,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry " +
            "FROM map_layer_feature WHERE layer_id = #{layerId} " +
            "AND geometry::geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326) " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJsonByBbox(@Param("layerId") String layerId,
                                                  @Param("minX") String minX,
                                                  @Param("minY") String minY,
                                                  @Param("maxX") String maxX,
                                                  @Param("maxY") String maxY,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry " +
            "FROM map_layer_feature WHERE layer_id = #{layerId} AND source_id = #{sourceId} " +
            "AND geometry::geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326) " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJsonBySourceAndBbox(@Param("layerId") String layerId,
                                                           @Param("sourceId") String sourceId,
                                                           @Param("minX") String minX,
                                                           @Param("minY") String minY,
                                                           @Param("maxX") String maxX,
                                                           @Param("maxY") String maxY,
                                                           @Param("limit") int limit,
                                                           @Param("offset") int offset);

    @Select("SELECT count(*) FROM map_layer_feature WHERE layer_id = #{layerId} " +
            "AND geometry::geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)")
    long countByLayerAndBbox(@Param("layerId") String layerId,
                             @Param("minX") String minX,
                             @Param("minY") String minY,
                             @Param("maxX") String maxX,
                             @Param("maxY") String maxY);

    @Select("SELECT count(*) FROM map_layer_feature WHERE layer_id = #{layerId} AND source_id = #{sourceId} " +
            "AND geometry::geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)")
    long countByLayerSourceAndBbox(@Param("layerId") String layerId,
                                   @Param("sourceId") String sourceId,
                                   @Param("minX") String minX,
                                   @Param("minY") String minY,
                                   @Param("maxX") String maxX,
                                   @Param("maxY") String maxY);

    // ── 要素聚类（ST_ClusterKMeans 不支持 geography，需转 geometry） ──

    @Select("SELECT ST_AsGeoJSON(ST_Centroid(ST_Collect(geometry::geometry))) as center, COUNT(*) as count FROM (SELECT geometry, ST_ClusterKMeans(geometry::geometry, #{k}) OVER () AS cluster_id FROM map_layer_feature WHERE layer_id = #{layerId}) t GROUP BY cluster_id")
    List<Map<String, Object>> cluster(@Param("layerId") String layerId, @Param("k") int k);

    @Select("SELECT ST_AsGeoJSON(ST_Centroid(ST_Collect(geometry::geometry))) as center, COUNT(*) as count FROM (SELECT geometry, ST_ClusterKMeans(geometry::geometry, #{k}) OVER () AS cluster_id FROM map_layer_feature WHERE layer_id = #{layerId} AND geometry::geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)) t GROUP BY cluster_id")
    List<Map<String, Object>> clusterWithBbox(@Param("layerId") String layerId, @Param("k") int k, @Param("minX") String minX, @Param("minY") String minY, @Param("maxX") String maxX, @Param("maxY") String maxY);

    // ── BM25 全文搜索 ──────────────────────────────────────

    @org.apache.ibatis.annotations.Select({
        "<script>",
        "SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry,",
        "       paradedb.score(id) AS relevance",
        "FROM map_layer_feature",
        "WHERE layer_id = #{layerId}",
        "  AND properties @@@ #{query}",
        "ORDER BY relevance DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Map<String, Object>> searchBm25(@Param("layerId") String layerId,
                                          @Param("query") String query,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @org.apache.ibatis.annotations.Select({
        "<script>",
        "SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry,",
        "       paradedb.score(id) AS relevance",
        "FROM map_layer_feature",
        "WHERE layer_id = #{layerId}",
        "  AND properties @@@ #{query}",
        "  AND geometry::geometry &amp;&amp; ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)",
        "ORDER BY relevance DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Map<String, Object>> searchBm25WithBbox(@Param("layerId") String layerId,
                                                  @Param("query") String query,
                                                  @Param("minX") String minX,
                                                  @Param("minY") String minY,
                                                  @Param("maxX") String maxX,
                                                  @Param("maxY") String maxY,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    // ── 带 filter + sort 的通用查询（properties JSONB 属性筛选） ──

    @Select({
        "<script>",
        "SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry",
        "FROM map_layer_feature",
        "WHERE layer_id = #{layerId}",
        "<if test='bbox != null'>",
        "  AND geometry::geometry &amp;&amp; ST_MakeEnvelope(CAST(#{bbox[0]} AS DOUBLE PRECISION), CAST(#{bbox[1]} AS DOUBLE PRECISION), CAST(#{bbox[2]} AS DOUBLE PRECISION), CAST(#{bbox[3]} AS DOUBLE PRECISION), 4326)",
        "</if>",
        "<foreach collection='query.filter' item='c' separator=''>",
        "  <choose>",
        "    <when test='c.op == \"eq\"'>AND properties->>'${c.field}' = #{c.value}</when>",
        "    <when test='c.op == \"neq\"'>AND properties->>'${c.field}' != #{c.value}</when>",
        "    <when test='c.op == \"like\"'>AND properties->>'${c.field}' ILIKE CONCAT('%', #{c.value}, '%')</when>",
        "    <when test='c.op == \"gt\"'>AND (properties->>'${c.field}')::numeric &gt; #{c.value}</when>",
        "    <when test='c.op == \"gte\"'>AND (properties->>'${c.field}')::numeric &gt;= #{c.value}</when>",
        "    <when test='c.op == \"lt\"'>AND (properties->>'${c.field}')::numeric &lt; #{c.value}</when>",
        "    <when test='c.op == \"lte\"'>AND (properties->>'${c.field}')::numeric &lt;= #{c.value}</when>",
        "    <when test='c.op == \"in\"'>",
        "      AND properties->>'${c.field}' IN",
        "      <foreach collection='c.value' item='v' open='(' separator=',' close=')'>#{v}</foreach>",
        "    </when>",
        "    <when test='c.op == \"isnull\"'>AND properties->>'${c.field}' IS NULL</when>",
        "    <when test='c.op == \"notnull\"'>AND properties->>'${c.field}' IS NOT NULL</when>",
        "  </choose>",
        "</foreach>",
        "<if test='query.sort != null and query.sort.size() > 0'>",
        "  ORDER BY",
        "  <foreach collection='query.sort' item='s' separator=', '>",
        "    (properties->>'${s.field}') ${s.order}",
        "  </foreach>",
        "</if>",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Map<String, Object>> findWithQuery(@Param("layerId") String layerId,
                                             @Param("bbox") double[] bbox,
                                             @Param("query") FeatureQuery query,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    // ── 几何合并（ST_Union，用于要素合并兜底计算） ──────────────

    @Select({
        "<script>",
        "SELECT ST_AsGeoJSON(ST_Union(geometry::geometry)) AS geometry",
        "FROM map_layer_feature",
        "WHERE layer_id = #{layerId} AND id IN",
        "<foreach collection='originIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    String unionGeometry(@Param("layerId") String layerId, @Param("originIds") List<Long> originIds);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM map_layer_feature",
        "WHERE layer_id = #{layerId}",
        "<if test='bbox != null'>",
        "  AND geometry::geometry &amp;&amp; ST_MakeEnvelope(CAST(#{bbox[0]} AS DOUBLE PRECISION), CAST(#{bbox[1]} AS DOUBLE PRECISION), CAST(#{bbox[2]} AS DOUBLE PRECISION), CAST(#{bbox[3]} AS DOUBLE PRECISION), 4326)",
        "</if>",
        "<foreach collection='query.filter' item='c' separator=''>",
        "  <choose>",
        "    <when test='c.op == \"eq\"'>AND properties->>'${c.field}' = #{c.value}</when>",
        "    <when test='c.op == \"neq\"'>AND properties->>'${c.field}' != #{c.value}</when>",
        "    <when test='c.op == \"like\"'>AND properties->>'${c.field}' ILIKE CONCAT('%', #{c.value}, '%')</when>",
        "    <when test='c.op == \"gt\"'>AND (properties->>'${c.field}')::numeric &gt; #{c.value}</when>",
        "    <when test='c.op == \"gte\"'>AND (properties->>'${c.field}')::numeric &gt;= #{c.value}</when>",
        "    <when test='c.op == \"lt\"'>AND (properties->>'${c.field}')::numeric &lt; #{c.value}</when>",
        "    <when test='c.op == \"lte\"'>AND (properties->>'${c.field}')::numeric &lt;= #{c.value}</when>",
        "    <when test='c.op == \"in\"'>",
        "      AND properties->>'${c.field}' IN",
        "      <foreach collection='c.value' item='v' open='(' separator=',' close=')'>#{v}</foreach>",
        "    </when>",
        "    <when test='c.op == \"isnull\"'>AND properties->>'${c.field}' IS NULL</when>",
        "    <when test='c.op == \"notnull\"'>AND properties->>'${c.field}' IS NOT NULL</when>",
        "  </choose>",
        "</foreach>",
        "</script>"
    })
    long countWithQuery(@Param("layerId") String layerId,
                        @Param("bbox") double[] bbox,
                        @Param("query") FeatureQuery query);
}
