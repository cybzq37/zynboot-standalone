package com.zynboot.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.infrastructure.mapper.MapSpatialMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapExportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LayerRepository layerRepository;
    private final MapSpatialMapper spatialMapper;

    public ExportPlan prepare(String layerId, String requestedFormat) {
        layerRepository.findById(layerId)
                .orElseThrow(() -> BizException.notFound("图层"));

        String format = requestedFormat == null ? "geojson" : requestedFormat.toLowerCase();
        return switch (format) {
            case "csv" -> new ExportPlan("csv", "text/csv; charset=UTF-8", "export_" + layerId + ".csv");
            case "geojson" -> new ExportPlan("geojson", "application/json", "export_" + layerId + ".geojson");
            default -> throw BizException.badRequest("仅支持导出 geojson 或 csv");
        };
    }

    public void write(String layerId, String sourceId, String format, OutputStream outputStream) throws Exception {
        switch (format) {
            case "csv" -> exportCsv(layerId, sourceId, outputStream);
            case "geojson" -> exportGeoJson(layerId, sourceId, outputStream);
            default -> throw BizException.badRequest("仅支持导出 geojson 或 csv");
        }
    }

    private void exportGeoJson(String layerId, String sourceId, OutputStream outputStream) throws Exception {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));
        writer.write("{\"type\":\"FeatureCollection\",\"features\":[");
        writer.flush();

        int page = 0;
        int pageSize = 500;
        boolean first = true;
        List<Map<String, Object>> features;

        do {
            features = fetchPage(layerId, sourceId, pageSize, page * pageSize);
            for (Map<String, Object> feature : features) {
                if (!first) {
                    writer.write(",");
                }
                writer.write(MAPPER.writeValueAsString(feature));
                first = false;
            }
            writer.flush();
            page++;
        } while (features.size() == pageSize);

        writer.write("]}");
        writer.flush();
    }

    private void exportCsv(String layerId, String sourceId, OutputStream outputStream) throws Exception {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));
        writer.println("id,source_id,lng,lat,properties");
        writer.flush();

        int page = 0;
        int pageSize = 500;
        List<Map<String, Object>> features;

        do {
            features = fetchPage(layerId, sourceId, pageSize, page * pageSize);
            for (Map<String, Object> feature : features) {
                String id = String.valueOf(feature.get("id"));
                String sourceIdVal = String.valueOf(feature.get("source_id"));
                String properties = feature.get("properties") != null
                        ? MAPPER.writeValueAsString(feature.get("properties")).replace("\"", "\"\"")
                        : "";
                String geomJson = feature.get("geometry") != null ? feature.get("geometry").toString() : "";
                String[] lngLat = extractLngLat(geomJson);
                writer.println(String.format("\"%s\",\"%s\",%s,%s,\"{\\\"properties\\\":%s}\"",
                        id, sourceIdVal, lngLat[0], lngLat[1], properties));
            }
            writer.flush();
            page++;
        } while (features.size() == pageSize);
    }

    private List<Map<String, Object>> fetchPage(String layerId, String sourceId, int limit, int offset) {
        if (sourceId != null && !sourceId.isBlank()) {
            return spatialMapper.findAsGeoJsonBySource(layerId, sourceId, limit, offset);
        }
        return spatialMapper.findAsGeoJson(layerId, limit, offset);
    }

    private String[] extractLngLat(String geoJson) {
        if (geoJson != null && geoJson.contains("\"coordinates\"")) {
            try {
                Map<String, Object> geom = MAPPER.readValue(geoJson, Map.class);
                Object coords = geom.get("coordinates");
                if (coords instanceof List<?> list && list.size() >= 2) {
                    return new String[]{String.valueOf(list.get(0)), String.valueOf(list.get(1))};
                }
            } catch (Exception e) {
                log.debug("Extract lng/lat failed", e);
            }
        }
        return new String[]{"0", "0"};
    }

    @Getter
    @AllArgsConstructor
    public static class ExportPlan {
        private final String format;
        private final String contentType;
        private final String filename;
    }
}
