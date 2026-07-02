package com.zynboot.map.response.instance;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class InstanceRes {
    String id;
    String name;
    String description;
    Double centerLng;
    Double centerLat;
    Integer zoom;
    String basemapId;
    Boolean isPublic;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
