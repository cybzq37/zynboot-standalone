package com.zynboot.infra.geo.shp;

import org.locationtech.jts.geom.Geometry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ShpFeatureData(
        String id,
        Map<String, Object> attributes,
        Geometry geometry
) {
    public ShpFeatureData {
        attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
