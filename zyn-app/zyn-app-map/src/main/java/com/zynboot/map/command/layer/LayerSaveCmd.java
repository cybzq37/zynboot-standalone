package com.zynboot.map.command.layer;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "图层保存参数")
public class LayerSaveCmd {

    private static final String GEOMETRY_TYPE_PATTERN =
            "POINT|LINESTRING|POLYGON|MULTIPOINT|MULTILINESTRING|MULTIPOLYGON|GEOMETRYCOLLECTION";

    @Schema(description = "图层 ID，更新时可传", example = "3d74d7a5f9b04b43a9b07b0fdc30e6bf")
    String id;
    @Schema(description = "图层所属分组 ID", example = "f4f7c0b84efb4c4b9e6f433d49bff1bd")
    String groupId;

    @Schema(description = "图层名称", example = "road_network")
    @NotBlank(message = "图层名称不能为空")
    String name;

    @Schema(description = "图层描述", example = "全市道路中心线数据")
    String description;

    /**
     * 图层类型，取值对应 {@link com.zynboot.map.domain.enums.LayerType}：RASTER（栅格）/ VECTOR（矢量）。
     */
    @Schema(description = "图层类型，可选 RASTER 或 VECTOR", example = "VECTOR")
    @NotBlank(message = "图层类型不能为空")
    @Pattern(regexp = "RASTER|VECTOR", message = "图层类型仅支持 RASTER 或 VECTOR")
    String type;

    @Schema(description = "目标坐标系 EPSG 编码", example = "3857")
    @NotNull(message = "目标坐标系不能为空")
    Integer targetSrid;

    /**
     * 几何类型，仅 type=VECTOR 时有意义，取 PostGIS/GeoJSON 几何类型名：
     * POINT / LINESTRING / POLYGON / MULTIPOINT / MULTILINESTRING / MULTIPOLYGON / GEOMETRYCOLLECTION。
     * type=RASTER 时留空。
     */
    @Schema(description = "几何类型，VECTOR 图层必填", example = "LINESTRING")
    @Pattern(regexp = GEOMETRY_TYPE_PATTERN, message = "几何类型仅支持 POINT/LINESTRING/POLYGON/MULTIPOINT/MULTILINESTRING/MULTIPOLYGON/GEOMETRYCOLLECTION")
    String geometryType;
    @Schema(description = "渲染顺序，越大越靠上", example = "100")
    Integer renderOrder;
    @Schema(description = "最小缩放级别", example = "8")
    Integer minZoom;
    @Schema(description = "最大缩放级别", example = "18")
    Integer maxZoom;
    @Schema(description = "透明度", example = "0.85")
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
