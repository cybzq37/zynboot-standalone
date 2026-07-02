package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_layer_version")
public class MapLayerVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String layerId;
    private Integer version;
    private String name;
    private String type;
    private String sourceSnapshot;
    private Integer featureCount;
    private Integer sourceCount;
    private String createdBy;
    private LocalDateTime createdAt;
}
