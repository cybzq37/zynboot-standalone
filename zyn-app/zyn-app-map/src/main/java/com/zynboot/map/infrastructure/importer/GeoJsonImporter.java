package com.zynboot.map.infrastructure.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON 导入器：解析 FeatureCollection，返回要素列表。
 */
@Slf4j
@Component
public class GeoJsonImporter implements VectorImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean supports(String format) {
        return "GEOJSON".equalsIgnoreCase(format) || "JSON".equalsIgnoreCase(format);
    }

    @Override
    public List<FeatureRecord> parse(InputStream input, String sourceSrid) throws Exception {
        List<FeatureRecord> records = new ArrayList<>();
        JsonNode root = MAPPER.readTree(input);

        String type = root.has("type") ? root.get("type").asText() : "";
        JsonNode features;

        if ("FeatureCollection".equals(type)) {
            features = root.get("features");
        } else if ("Feature".equals(type)) {
            features = MAPPER.createArrayNode().add(root);
        } else {
            // Single geometry
            String geomJson = MAPPER.writeValueAsString(root);
            records.add(new FeatureRecord("{}", geomJson));
            return records;
        }

        if (features == null) return records;

        for (JsonNode feature : features) {
            JsonNode geometry = feature.get("geometry");
            JsonNode properties = feature.get("properties");

            if (geometry == null) continue;

            String geomJson = MAPPER.writeValueAsString(geometry);
            String propsJson = properties != null && !properties.isNull()
                    ? MAPPER.writeValueAsString(properties) : "{}";

            records.add(new FeatureRecord(propsJson, geomJson));
        }

        log.info("GeoJSON parsed: {} features", records.size());
        return records;
    }
}
