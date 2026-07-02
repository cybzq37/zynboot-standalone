package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_feature")
public class MapFeature implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String layerId;
    private String sourceId;
    private String properties;
    private String geometry;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
