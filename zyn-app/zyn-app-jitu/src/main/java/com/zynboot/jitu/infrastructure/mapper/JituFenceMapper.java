package com.zynboot.jitu.infrastructure.mapper;

import com.zynboot.jitu.infrastructure.entity.JituFenceHit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JituFenceMapper {

    @Select("SELECT id, properties::text AS properties, ST_AsGeoJSON(geometry) AS geometry, " +
            "ST_X(ST_PointOnSurface(geometry)) AS centerLng, " +
            "ST_Y(ST_PointOnSurface(geometry)) AS centerLat " +
            "FROM map_feature " +
            "WHERE layer_id = #{layerId} " +
            "AND geometry && ST_SetSRID(ST_Point(CAST(#{lng} AS DOUBLE PRECISION), CAST(#{lat} AS DOUBLE PRECISION)), 4326) " +
            "AND ST_Covers(geometry, ST_SetSRID(ST_Point(CAST(#{lng} AS DOUBLE PRECISION), CAST(#{lat} AS DOUBLE PRECISION)), 4326)) " +
            "ORDER BY id " +
            "LIMIT 1")
    JituFenceHit findFirstByPoint(@Param("layerId") String layerId,
                                  @Param("lng") double lng,
                                  @Param("lat") double lat);
}
