package com.zynboot.map.response.layer;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LayerStyleRes {
    String id;
    String layerId;
    String name;
    String type;
    String styleJson;
    Boolean isDefault;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
