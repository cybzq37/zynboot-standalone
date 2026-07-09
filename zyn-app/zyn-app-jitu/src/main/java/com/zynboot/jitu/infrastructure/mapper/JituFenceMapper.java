package com.zynboot.jitu.infrastructure.mapper;

import com.zynboot.jitu.infrastructure.entity.JituFenceHit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JituFenceMapper {

    /**
     * 用经纬度坐标在指定图层中查找命中的面要素（点在面内），并通过 properties.type 区分取件/收件。
     * map_layer_feature.geometry 为 GEOGRAPHY 类型，空间操作符需 ::geometry 转换。
     * properties.type 为业务类型（1=取件，2=收件）。
     */
    @Select("SELECT id, properties::text AS properties, ST_AsGeoJSON(geometry) AS geometry, " +
            "ST_X(ST_PointOnSurface(geometry::geometry)) AS centerLng, " +
            "ST_Y(ST_PointOnSurface(geometry::geometry)) AS centerLat " +
            "FROM map_layer_feature " +
            "WHERE layer_id = #{layerId} " +
            "AND properties->>'type' = #{type} " +
            "AND geometry::geometry && ST_SetSRID(ST_Point(CAST(#{lng} AS DOUBLE PRECISION), CAST(#{lat} AS DOUBLE PRECISION)), 4326) " +
            "AND ST_Covers(geometry::geometry, ST_SetSRID(ST_Point(CAST(#{lng} AS DOUBLE PRECISION), CAST(#{lat} AS DOUBLE PRECISION)), 4326)) " +
            "ORDER BY id " +
            "LIMIT 1")
    JituFenceHit findFirstByPoint(@Param("layerId") String layerId,
                                  @Param("type") String type,
                                  @Param("lng") double lng,
                                  @Param("lat") double lat);
}
