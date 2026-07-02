package com.zynboot.map.command.basemap;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BasemapSaveCmd {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String type;
    @NotBlank
    private String url;
    private Integer srid;
    private String attribution;
    private Integer minZoom;
    private Integer maxZoom;
    private String thumbnailUrl;
    private Boolean isDefault;
    private Integer sortOrder;
    private String wmsLayers;
    private String wmtsLayer;
    private String wmtsStyle;
    private String wmtsMatrixSet;
}
