package com.zynboot.map.response.version;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LayerVersionRes {
    String id;
    String layerId;
    Integer version;
    String name;
    String type;
    String sourceSnapshot;
    Integer featureCount;
    Integer sourceCount;
    String createdBy;
    LocalDateTime createdAt;
}
