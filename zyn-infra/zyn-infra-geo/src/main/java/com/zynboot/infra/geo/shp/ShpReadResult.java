package com.zynboot.infra.geo.shp;

import java.util.List;

public record ShpReadResult(
        ShpSchema schema,
        List<ShpFeatureData> features
) {
    public ShpReadResult {
        features = features == null ? List.of() : List.copyOf(features);
    }
}
