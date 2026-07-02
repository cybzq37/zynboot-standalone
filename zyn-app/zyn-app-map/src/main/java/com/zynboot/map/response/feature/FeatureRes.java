package com.zynboot.map.response.feature;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FeatureRes {
    Long id;
    String layerId;
    String sourceId;
    String properties;
    String geometry;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
