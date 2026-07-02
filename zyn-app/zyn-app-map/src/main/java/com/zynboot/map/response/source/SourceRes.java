package com.zynboot.map.response.source;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SourceRes {
    String id;
    String layerId;
    String name;
    String type;
    String format;
    Integer sourceSrid;
    Integer targetSrid;
    Integer featureCount;
    String status;
    String storageKey;
    String dataSourceId;
    String externalTable;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
