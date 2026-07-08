package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_layer")
public class MapLayer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String groupId;
    private String name;
    private String description;
    private String type;
    private String geometryType;
    private Integer featureCount;
    private Integer sourceCount;
    private Integer renderOrder;
    private Boolean visible;
    private Boolean selectable;
    private Boolean editable;
    private Boolean isPublic;
    private Integer minZoom;
    private Integer maxZoom;
    private Double opacity;
    private String styleId;
    private String metadata;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
