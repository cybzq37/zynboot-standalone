package com.zynboot.map.response.instance;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceLayerRes {
    String id;
    String instanceId;
    String parentId;
    Boolean isGroup;
    String layerId;
    String name;
    Boolean visible;
    Double opacity;
    Integer renderOrder;
}
