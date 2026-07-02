package com.zynboot.infra.geo.shp;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ShpSchemaMapper {

    private ShpSchemaMapper() {
    }

    static ShpSchema buildSchema(String typeName, SimpleFeatureType featureType, ShpReadOptions options) {
        Map<String, Class<?>> rawFields = new LinkedHashMap<>();
        for (AttributeDescriptor descriptor : featureType.getAttributeDescriptors()) {
            if (!Geometry.class.isAssignableFrom(descriptor.getType().getBinding())) {
                rawFields.put(descriptor.getLocalName(), descriptor.getType().getBinding());
            }
        }
        return buildSchema(typeName, rawFields, options);
    }

    static ShpSchema buildSchema(String typeName, Map<String, Class<?>> rawFields, ShpReadOptions options) {
        ShpReadOptions readOptions = options == null ? ShpReadOptions.defaults() : options;
        List<ShpFieldMeta> fields = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for (Map.Entry<String, Class<?>> entry : rawFields.entrySet()) {
            String rawName = entry.getKey();
            String baseName = readOptions.normalizeFieldNames()
                    ? ShpFieldNameNormalizer.normalize(rawName)
                    : rawName;
            String resolvedName = resolveUniqueName(baseName, usedNames);
            fields.add(new ShpFieldMeta(rawName, resolvedName, entry.getValue()));
        }
        return new ShpSchema(typeName, fields);
    }

    static ShpFeatureData buildFeatureData(
            String id,
            Map<String, Object> rawAttributes,
            Geometry geometry,
            Map<String, String> fieldNameMapping
    ) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawAttributes.entrySet()) {
            String normalizedName = fieldNameMapping.getOrDefault(entry.getKey(), entry.getKey());
            attributes.put(normalizedName, entry.getValue());
        }
        return new ShpFeatureData(id, attributes, geometry);
    }

    private static final int MAX_NAME_DEDUP_RETRIES = 9999;

    private static String resolveUniqueName(String baseName, Set<String> usedNames) {
        String candidate = (baseName == null || baseName.isBlank()) ? "field" : baseName;
        if (usedNames.add(candidate)) {
            return candidate;
        }

        for (int suffix = 2; suffix <= MAX_NAME_DEDUP_RETRIES; suffix++) {
            String resolved = appendSuffix(candidate, suffix);
            if (usedNames.add(resolved)) {
                return resolved;
            }
        }
        throw new IllegalStateException("Failed to resolve unique field name for: " + baseName);
    }

    private static String appendSuffix(String baseName, int suffix) {
        if (!baseName.isEmpty() && Character.isDigit(baseName.charAt(baseName.length() - 1))) {
            return baseName + "_" + suffix;
        }
        return baseName + suffix;
    }
}
