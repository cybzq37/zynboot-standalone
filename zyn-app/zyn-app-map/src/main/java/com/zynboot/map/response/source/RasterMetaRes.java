package com.zynboot.map.response.source;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RasterMetaRes {
    String sourceId;
    String path;
    Integer width;
    Integer height;
    Integer bands;
    String dataType;
    Double nodata;
    Double pixelSizeX;
    Double pixelSizeY;
    Long compressedBytes;
    Long uncompressedBytes;
}
