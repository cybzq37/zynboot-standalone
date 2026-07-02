package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_publish")
public class MapPublish implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String instanceId;
    private String type;
    private Boolean isActive;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
