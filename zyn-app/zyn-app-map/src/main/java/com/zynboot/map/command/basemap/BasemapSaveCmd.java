package com.zynboot.map.command.basemap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "底图保存参数")
public class BasemapSaveCmd {
    @Schema(description = "底图名称", example = "天地图矢量底图")
    @NotBlank
    private String name;
    @Schema(description = "底图描述", example = "适用于业务地图展示的全国矢量底图")
    private String description;
    @Schema(description = "底图类型", example = "XYZ")
    @NotBlank
    private String type;
    @Schema(description = "底图服务地址", example = "https://tile.example.com/{z}/{x}/{y}.png")
    @NotBlank
    private String url;
    @Schema(description = "底图坐标系 EPSG 编码", example = "3857")
    private Integer srid;
    @Schema(description = "版权归属说明", example = "Map data © provider")
    private String attribution;
    @Schema(description = "最小缩放级别", example = "0")
    private Integer minZoom;
    @Schema(description = "最大缩放级别", example = "18")
    private Integer maxZoom;
    @Schema(description = "缩略图地址", example = "https://tile.example.com/thumbnail.png")
    private String thumbnailUrl;
    @Schema(description = "是否设为默认底图", example = "true")
    private Boolean isDefault;
    @Schema(description = "排序值，越小越靠前", example = "10")
    private Integer sortOrder;
    @Schema(description = "WMS 图层名，type=WMS 时使用", example = "basemap")
    private String wmsLayers;
    @Schema(description = "WMTS 图层名，type=WMTS 时使用", example = "vec")
    private String wmtsLayer;
    @Schema(description = "WMTS 样式名", example = "default")
    private String wmtsStyle;
    @Schema(description = "WMTS 矩阵集", example = "GoogleMapsCompatible")
    private String wmtsMatrixSet;
}
