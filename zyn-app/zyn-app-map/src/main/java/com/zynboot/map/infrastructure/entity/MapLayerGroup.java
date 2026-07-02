package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_layer_group")
public class MapLayerGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String parentId;
    private String name;
    private String description;
    private Integer sortOrder;
    private String icon;
    private String color;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
