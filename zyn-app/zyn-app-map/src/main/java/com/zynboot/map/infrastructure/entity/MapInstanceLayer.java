package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("map_instance_layer")
public class MapInstanceLayer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String instanceId;
    private String parentId;
    private Boolean isGroup;
    private String layerId;
    private String name;
    private Boolean visible;
    private Double opacity;
    private Integer renderOrder;
}
