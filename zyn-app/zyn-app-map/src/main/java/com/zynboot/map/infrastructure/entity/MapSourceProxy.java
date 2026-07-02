package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_source_proxy")
public class MapSourceProxy implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("source_id")
    private String sourceId;
    private String url;
    private String wmtsLayer;
    private String wmtsStyle;
    private String wmtsMatrixSet;
    private String wmtsFormat;
    private String authType;
    private String authHeader;
    private String authValue;
    private Integer cacheTtl;
    private String healthStatus;
    private String healthMessage;
    private LocalDateTime lastCheckAt;
    private Integer failCount;
}
