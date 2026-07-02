package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_layer_source")
public class MapLayerSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String layerId;
    private String name;
    private String type;
    private String format;
    private Integer sourceSrid;
    private Integer targetSrid;
    private String storageKey;
    private String geometryType;
    private Integer featureCount;
    private String fieldMapping;
    private String status;
    private String message;
    private String dataSourceId;
    private String externalSchema;
    private String externalTable;
    private String externalGeomCol;
    private String externalIdCol;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
