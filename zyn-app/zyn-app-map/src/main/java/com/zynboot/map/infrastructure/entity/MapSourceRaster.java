package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("map_source_raster")
public class MapSourceRaster implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("source_id")
    private String sourceId;
    private String path;
    private Integer width;
    private Integer height;
    private Integer bands;
    private String dataType;
    private Double nodata;
    private Double pixelSizeX;
    private Double pixelSizeY;
    private Long compressedBytes;
    private Long uncompressedBytes;
}
