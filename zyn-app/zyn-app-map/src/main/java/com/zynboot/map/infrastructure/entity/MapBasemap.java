package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_basemap")
public class MapBasemap implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String name;
    private String description;
    private String type;
    private String url;
    private Integer srid;
    private String attribution;
    private Integer minZoom;
    private Integer maxZoom;
    private String thumbnailUrl;
    private Boolean isDefault;
    private Integer sortOrder;
    private String wmsLayers;
    private String wmtsLayer;
    private String wmtsStyle;
    private String wmtsMatrixSet;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
