package com.zynboot.map.response.layer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LayerFieldRes {
    String id;
    String layerId;
    String name;
    String alias;
    String type;
    Boolean visible;
    Boolean sortable;
    Boolean searchable;
    Integer sortOrder;
}
