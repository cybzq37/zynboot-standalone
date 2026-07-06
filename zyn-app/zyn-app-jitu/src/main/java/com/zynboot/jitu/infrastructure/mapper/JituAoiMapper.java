package com.zynboot.jitu.infrastructure.mapper;

import com.zynboot.jitu.infrastructure.entity.JituAoiView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JituAoiMapper {

    @Select("SELECT id, aoi_type AS aoiType, aoi_name AS aoiName, address, gb_code AS gbCode, kind, " +
            "longitude, latitude, properties::text AS properties, " +
            "COALESCE(geojson::text, ST_AsGeoJSON(geometry)) AS geojson " +
            "FROM lbs_aoi WHERE id = #{id}")
    JituAoiView selectById(@Param("id") String id);
}
