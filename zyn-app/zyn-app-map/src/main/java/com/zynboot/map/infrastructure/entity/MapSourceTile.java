package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("map_source_tile")
public class MapSourceTile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("source_id")
    private String sourceId;
    private String status;
    private String path;
    private Integer minZoom;
    private Integer maxZoom;
    private String format;
    private Integer tileSize;
    private Integer tileCount;
    private Integer progress;
}
