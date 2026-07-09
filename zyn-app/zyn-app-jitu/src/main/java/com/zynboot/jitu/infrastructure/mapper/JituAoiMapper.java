package com.zynboot.jitu.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JituAoiMapper {

    /**
     * 在 lbs_aoi_bp 表中用经纬度坐标查找命中的面（点在面内），返回 GeoJSON 字符串。
     * geometry 为 GEOGRAPHY 类型，ST_Covers 需 ::geometry 转换。
     */
    @Select("SELECT ST_AsGeoJSON(geometry::geometry) AS geojson " +
            "FROM lbs_aoi_bp " +
            "WHERE ST_Covers(geometry, ST_SetSRID(ST_Point(CAST(#{lng} AS DOUBLE PRECISION), CAST(#{lat} AS DOUBLE PRECISION)), 4326)) " +
            "ORDER BY id LIMIT 1")
    String findFirstBpGeojsonByPoint(@Param("lng") double lng,
                                     @Param("lat") double lat);

    /**
     * 在 lbs_aoi_building 表中用经纬度坐标查找命中的面（点在面内），返回 GeoJSON 字符串。
     * geometry 为 GEOGRAPHY 类型，ST_Covers 需 ::geometry 转换。
     */
    @Select("SELECT ST_AsGeoJSON(geometry::geometry) AS geojson " +
            "FROM lbs_aoi_building " +
            "WHERE ST_Covers(geometry, ST_SetSRID(ST_Point(CAST(#{lng} AS DOUBLE PRECISION), CAST(#{lat} AS DOUBLE PRECISION)), 4326)) " +
            "ORDER BY id LIMIT 1")
    String findFirstBuildingGeojsonByPoint(@Param("lng") double lng,
                                           @Param("lat") double lat);
}
