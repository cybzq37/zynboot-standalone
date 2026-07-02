package com.zynboot.map.infrastructure.mapper;

import com.zynboot.map.infrastructure.entity.MapFeature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MapSpatialMapper {

    // ── 空间分析 ───────────────────────────────────────────

    @Select("SELECT ST_AsGeoJSON(ST_Buffer(geometry::geography, #{distanceMeters})::geometry) FROM map_feature WHERE id = CAST(#{featureId} AS BIGINT)")
    String buffer(@Param("featureId") String featureId, @Param("distanceMeters") double distanceMeters);

    @Select("SELECT ST_Distance(a.geometry::geography, b.geometry::geography) FROM map_feature a, map_feature b WHERE a.id = CAST(#{id1} AS BIGINT) AND b.id = CAST(#{id2} AS BIGINT)")
    double distance(@Param("id1") String featureId1, @Param("id2") String featureId2);

    @Select("SELECT ST_Area(geometry::geography) FROM map_feature WHERE id = CAST(#{featureId} AS BIGINT)")
    double area(@Param("featureId") String featureId);

    @Select("SELECT ST_Intersects(a.geometry, b.geometry) FROM map_feature a, map_feature b WHERE a.id = CAST(#{id1} AS BIGINT) AND b.id = CAST(#{id2} AS BIGINT)")
    boolean intersects(@Param("id1") String featureId1, @Param("id2") String featureId2);

    @Select("SELECT f.id, f.properties, ST_AsGeoJSON(f.geometry) as geometry FROM map_feature f WHERE f.layer_id = #{layerId1} AND EXISTS (SELECT 1 FROM map_feature g WHERE g.layer_id = #{layerId2} AND ST_Intersects(f.geometry, g.geometry))")
    List<Map<String, Object>> intersection(@Param("layerId1") String layerId1, @Param("layerId2") String layerId2);

    // ── 空间查询 ───────────────────────────────────────────

    @Select("SELECT * FROM map_feature WHERE layer_id = #{layerId} AND geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326) LIMIT #{limit} OFFSET #{offset}")
    List<MapFeature> findByBbox(@Param("layerId") String layerId, @Param("minX") String minX, @Param("minY") String minY, @Param("maxX") String maxX, @Param("maxY") String maxY, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry, create_by, create_time, update_by, update_time FROM map_feature WHERE layer_id = #{layerId} LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJson(@Param("layerId") String layerId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry, create_by, create_time, update_by, update_time " +
            "FROM map_feature WHERE layer_id = #{layerId} AND source_id = #{sourceId} LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJsonBySource(@Param("layerId") String layerId,
                                                    @Param("sourceId") String sourceId,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry, create_by, create_time, update_by, update_time " +
            "FROM map_feature WHERE layer_id = #{layerId} " +
            "AND geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326) " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJsonByBbox(@Param("layerId") String layerId,
                                                  @Param("minX") String minX,
                                                  @Param("minY") String minY,
                                                  @Param("maxX") String maxX,
                                                  @Param("maxY") String maxY,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    @Select("SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry, create_by, create_time, update_by, update_time " +
            "FROM map_feature WHERE layer_id = #{layerId} AND source_id = #{sourceId} " +
            "AND geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326) " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findAsGeoJsonBySourceAndBbox(@Param("layerId") String layerId,
                                                           @Param("sourceId") String sourceId,
                                                           @Param("minX") String minX,
                                                           @Param("minY") String minY,
                                                           @Param("maxX") String maxX,
                                                           @Param("maxY") String maxY,
                                                           @Param("limit") int limit,
                                                           @Param("offset") int offset);

    @Select("SELECT count(*) FROM map_feature WHERE layer_id = #{layerId} " +
            "AND geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)")
    long countByLayerAndBbox(@Param("layerId") String layerId,
                             @Param("minX") String minX,
                             @Param("minY") String minY,
                             @Param("maxX") String maxX,
                             @Param("maxY") String maxY);

    @Select("SELECT count(*) FROM map_feature WHERE layer_id = #{layerId} AND source_id = #{sourceId} " +
            "AND geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)")
    long countByLayerSourceAndBbox(@Param("layerId") String layerId,
                                   @Param("sourceId") String sourceId,
                                   @Param("minX") String minX,
                                   @Param("minY") String minY,
                                   @Param("maxX") String maxX,
                                   @Param("maxY") String maxY);

    // ── 要素聚类 ───────────────────────────────────────────

    @Select("SELECT ST_AsGeoJSON(ST_Centroid(ST_Collect(geometry))) as center, COUNT(*) as count FROM (SELECT geometry, ST_ClusterKMeans(geometry, #{k}) OVER () AS cluster_id FROM map_feature WHERE layer_id = #{layerId}) t GROUP BY cluster_id")
    List<Map<String, Object>> cluster(@Param("layerId") String layerId, @Param("k") int k);

    @Select("SELECT ST_AsGeoJSON(ST_Centroid(ST_Collect(geometry))) as center, COUNT(*) as count FROM (SELECT geometry, ST_ClusterKMeans(geometry, #{k}) OVER () AS cluster_id FROM map_feature WHERE layer_id = #{layerId} AND geometry && ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)) t GROUP BY cluster_id")
    List<Map<String, Object>> clusterWithBbox(@Param("layerId") String layerId, @Param("k") int k, @Param("minX") String minX, @Param("minY") String minY, @Param("maxX") String maxX, @Param("maxY") String maxY);

    // ── BM25 全文搜索 ──────────────────────────────────────

    @org.apache.ibatis.annotations.Select({
        "<script>",
        "SELECT id, layer_id, source_id, properties, ST_AsGeoJSON(geometry) as geometry,",
        "       paradedb.score(id) AS relevance",
        "FROM map_feature",
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
        "FROM map_feature",
        "WHERE layer_id = #{layerId}",
        "  AND properties @@@ #{query}",
        "  AND geometry &amp;&amp; ST_MakeEnvelope(CAST(#{minX} AS DOUBLE PRECISION), CAST(#{minY} AS DOUBLE PRECISION), CAST(#{maxX} AS DOUBLE PRECISION), CAST(#{maxY} AS DOUBLE PRECISION), 4326)",
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
}
