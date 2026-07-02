package com.zynboot.infra.geo.shp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public record ShpSchema(
        String typeName,
        List<ShpFieldMeta> fields
) {
    private static final Map<ShpSchema, Map<String, String>> FIELD_MAPPING_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public ShpSchema {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public Map<String, String> fieldNameMapping() {
        return FIELD_MAPPING_CACHE.computeIfAbsent(this, schema -> {
            if (schema.fields.isEmpty()) {
                return Map.of();
            }
            Map<String, String> mapping = new LinkedHashMap<>();
            for (ShpFieldMeta field : schema.fields) {
                mapping.put(field.originalName(), field.normalizedName());
            }
            return Collections.unmodifiableMap(mapping);
        });
    }
}
