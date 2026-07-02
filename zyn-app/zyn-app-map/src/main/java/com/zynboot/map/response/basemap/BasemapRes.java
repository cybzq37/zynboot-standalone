package com.zynboot.map.response.basemap;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class BasemapRes {
    String id;
    String name;
    String description;
    String type;
    String url;
    Integer srid;
    String attribution;
    Integer minZoom;
    Integer maxZoom;
    String thumbnailUrl;
    Boolean isDefault;
    Integer sortOrder;
    String wmsLayers;
    String wmtsLayer;
    String wmtsStyle;
    String wmtsMatrixSet;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
