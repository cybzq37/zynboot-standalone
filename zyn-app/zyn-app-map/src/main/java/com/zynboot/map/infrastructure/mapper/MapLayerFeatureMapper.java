package com.zynboot.map.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zynboot.map.infrastructure.entity.MapLayerFeature;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MapLayerFeatureMapper extends BaseMapper<MapLayerFeature> {

    @Select("SELECT count(*) FROM map_layer_feature WHERE layer_id = #{layerId}")
    long countByLayerId(@Param("layerId") String layerId);

    @Select("SELECT count(*) FROM map_layer_feature WHERE source_id = #{sourceId}")
    long countBySourceId(@Param("sourceId") String sourceId);

    /**
     * 带 PostGIS 几何写入的插入。
     * GeoJSON 规范为 WGS84（4326），用 ST_GeomFromGeoJSON 解析后 ::geography 转为 geography 类型。
     * center 字段由 ST_Centroid 计算（geography 版本返回 geography Point）。
     */
    @Insert("INSERT INTO map_layer_feature (id, layer_id, source_id, properties, geometry, center) " +
            "VALUES (#{id}, #{layerId}, #{sourceId}, #{properties}::jsonb, " +
            "ST_GeomFromGeoJSON(#{geometryGeoJson}::text)::geography, " +
            "ST_Centroid(ST_GeomFromGeoJSON(#{geometryGeoJson}::text)::geography))")
    void insertWithGeometry(@Param("id") long id,
                            @Param("layerId") String layerId,
                            @Param("sourceId") String sourceId,
                            @Param("properties") String propertiesJson,
                            @Param("geometryGeoJson") String geometryGeoJson);

    @Update("UPDATE map_layer_feature SET source_id = #{sourceId}, properties = #{properties}::jsonb, " +
            "geometry = ST_GeomFromGeoJSON(#{geometryGeoJson}::text)::geography, " +
            "center = ST_Centroid(ST_GeomFromGeoJSON(#{geometryGeoJson}::text)::geography) " +
            "WHERE id = #{id}")
    int updateWithGeometry(@Param("id") long id,
                           @Param("sourceId") String sourceId,
                           @Param("properties") String propertiesJson,
                           @Param("geometryGeoJson") String geometryGeoJson);

    @Select("SELECT id, layer_id, source_id, properties::text AS properties, ST_AsGeoJSON(geometry) AS geometry, " +
            "ST_AsGeoJSON(center) AS center FROM map_layer_feature WHERE id = #{id}")
    MapLayerFeature selectViewById(@Param("id") long id);

    @Delete("DELETE FROM map_layer_feature WHERE id = #{id}")
    int deleteByIdValue(@Param("id") long id);

    @Delete("DELETE FROM map_layer_feature WHERE id = #{id} AND layer_id = #{layerId}")
    int deleteByLayerIdAndId(@Param("layerId") String layerId, @Param("id") long id);

    /**
     * 获取图层特征的 ETag（基于 MAX(id) + feature_count，用于缓存失效判断）。
     */
    @Select("SELECT MD5(COALESCE(MAX(id)::text, '') || ':' || COUNT(*)::text) FROM map_layer_feature WHERE layer_id = #{layerId}")
    String getLayerEtag(@Param("layerId") String layerId);
}
