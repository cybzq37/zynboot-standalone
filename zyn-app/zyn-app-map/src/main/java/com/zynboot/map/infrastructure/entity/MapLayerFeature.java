package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("map_layer_feature")
public class MapLayerFeature implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String layerId;
    private String sourceId;
    private String properties;
    private String geometry;
    private String center;
}
