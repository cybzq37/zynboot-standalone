package com.zynboot.map.response.source;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SourceTileRes {
    String sourceId;
    String status;
    String path;
    Integer minZoom;
    Integer maxZoom;
    String format;
    Integer tileSize;
    Integer tileCount;
    Integer progress;
}
