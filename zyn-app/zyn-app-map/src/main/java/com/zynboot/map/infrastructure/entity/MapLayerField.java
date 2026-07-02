package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_layer_field")
public class MapLayerField implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String layerId;
    private String name;
    private String alias;
    private String type;
    private Boolean visible;
    private Boolean sortable;
    private Boolean searchable;
    private Integer sortOrder;
}
