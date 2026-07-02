package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_instance")
public class MapInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String name;
    private String description;
    private Double centerLng;
    private Double centerLat;
    private Integer zoom;
    private String basemapId;
    private Boolean isPublic;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
