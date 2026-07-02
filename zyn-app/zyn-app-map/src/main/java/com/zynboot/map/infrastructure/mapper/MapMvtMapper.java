package com.zynboot.map.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 矢量瓦片 Mapper（PostGIS ST_AsMVT）。
 */
@Mapper
public interface MapMvtMapper {

    /**
     * 生成 MVT 瓦片。
     *
     * @param layerId 图层 ID
     * @param z       缩放级别
     * @param x       列号
     * @param y       行号
     * @param srid    目标 SRID（如 3857）
     * @param maxFeatures 单瓦片最大要素数（防止瓦片过大）
     * @return MVT 二进制（byte[]）
     */
    @org.apache.ibatis.annotations.Select({
        "<script>",
        "SELECT ST_AsMVT(tile, 'features', 4096, 'geom') FROM (",
        "  SELECT",
        "    f.id,",
        "    f.source_id,",
        "    f.properties,",
        "    ST_AsMVTGeom(",
        "      CASE WHEN #{srid} != 4326 THEN ST_Transform(f.geometry, #{srid}) ELSE f.geometry END,",
        "      ST_TileEnvelope(#{z}, #{x}, #{y}),",
        "      4096, 0, true",
        "    ) AS geom",
        "  FROM map_feature f",
        "  WHERE f.layer_id = #{layerId}",
        "    AND f.geometry && ST_Transform(ST_TileEnvelope(#{z}, #{x}, #{y}), 4326)",
        "  LIMIT #{maxFeatures}",
        ") tile",
        "</script>"
    })
    byte[] generateMvt(@Param("layerId") String layerId,
                       @Param("z") int z,
                       @Param("x") int x,
                       @Param("y") int y,
                       @Param("srid") int srid,
                       @Param("maxFeatures") int maxFeatures);

    /**
     * 检查瓦片范围内是否有要素（快速判断是否为空瓦片）。
     */
    @org.apache.ibatis.annotations.Select({
        "SELECT EXISTS(",
        "  SELECT 1 FROM map_feature",
        "  WHERE layer_id = #{layerId}",
        "    AND geometry && ST_Transform(ST_TileEnvelope(#{z}, #{x}, #{y}), 4326)",
        ")"
    })
    boolean hasFeatures(@Param("layerId") String layerId,
                        @Param("z") int z,
                        @Param("x") int x,
                        @Param("y") int y);

    /**
     * 获取图层特征的 ETag（基于 MAX(update_time) + feature_count）。
     * 用于缓存失效判断。
     */
    @org.apache.ibatis.annotations.Select({
        "SELECT MD5(COALESCE(MAX(update_time)::text, '') || ':' || COUNT(*)::text)",
        "FROM map_feature WHERE layer_id = #{layerId}"
    })
    String getLayerEtag(@Param("layerId") String layerId);
}
