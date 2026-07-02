package com.zynboot.map.command.layer;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LayerSaveCmd {

    private static final String GEOMETRY_TYPE_PATTERN =
            "POINT|LINESTRING|POLYGON|MULTIPOINT|MULTILINESTRING|MULTIPOLYGON|GEOMETRYCOLLECTION";

    String id;
    String groupId;

    @NotBlank(message = "图层名称不能为空")
    String name;

    String title;
    String description;

    /**
     * 图层类型，取值对应 {@link com.zynboot.map.domain.enums.LayerType}：RASTER（栅格）/ VECTOR（矢量）。
     */
    @NotBlank(message = "图层类型不能为空")
    @Pattern(regexp = "RASTER|VECTOR", message = "图层类型仅支持 RASTER 或 VECTOR")
    String type;

    @NotNull(message = "目标坐标系不能为空")
    Integer targetSrid;

    /**
     * 几何类型，仅 type=VECTOR 时有意义，取 PostGIS/GeoJSON 几何类型名：
     * POINT / LINESTRING / POLYGON / MULTIPOINT / MULTILINESTRING / MULTIPOLYGON / GEOMETRYCOLLECTION。
     * type=RASTER 时留空。
     */
    @Pattern(regexp = GEOMETRY_TYPE_PATTERN, message = "几何类型仅支持 POINT/LINESTRING/POLYGON/MULTIPOINT/MULTILINESTRING/MULTIPOLYGON/GEOMETRYCOLLECTION")
    String geometryType;
    Integer renderOrder;
    Integer minZoom;
    Integer maxZoom;
    Double opacity;

    @AssertTrue(message = "type=VECTOR 时 geometryType 不能为空；type=RASTER 时 geometryType 应留空")
    private boolean isGeometryTypeConsistentWithLayerType() {
        if ("VECTOR".equals(type)) {
            return geometryType != null && !geometryType.isBlank();
        }
        if ("RASTER".equals(type)) {
            return geometryType == null || geometryType.isBlank();
        }
        return true;
    }
}
