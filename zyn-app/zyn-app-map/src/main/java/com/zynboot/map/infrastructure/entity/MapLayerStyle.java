package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_layer_style")
public class MapLayerStyle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String layerId;
    private String name;
    private String type;
    private String styleJson;
    private Boolean isDefault;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
