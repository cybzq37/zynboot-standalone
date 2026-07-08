package com.zynboot.map.response.feature;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeatureRes {
    Long id;
    String layerId;
    String sourceId;
    String properties;
    String geometry;
    String center;
}
