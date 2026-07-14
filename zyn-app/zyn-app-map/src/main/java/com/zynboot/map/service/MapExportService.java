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
import java.util.LinkedHashMap;
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

    public void write(String layerId, String format, OutputStream outputStream) throws Exception {
        switch (format) {
            case "csv" -> exportCsv(layerId, outputStream);
            case "geojson" -> exportGeoJson(layerId, outputStream);
            default -> throw BizException.badRequest("仅支持导出 geojson 或 csv");
        }
    }

    private void exportGeoJson(String layerId, OutputStream outputStream) throws Exception {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));
        writer.write("{\"type\":\"FeatureCollection\",\"features\":[");
        writer.flush();

        int page = 0;
        int pageSize = 500;
        boolean first = true;
        List<Map<String, Object>> features;

        do {
            features = spatialMapper.findAsGeoJson(layerId, pageSize, page * pageSize);
            for (Map<String, Object> feature : features) {
                if (!first) {
                    writer.write(",");
                }
                // 构建标准 GeoJSON Feature 对象
                Map<String, Object> featureObj = new LinkedHashMap<>();
                featureObj.put("type", "Feature");
                featureObj.put("id", feature.get("id"));
                // properties 从字符串解析为 JSON 对象
                Object propsRaw = feature.get("properties");
                Object props = propsRaw != null ? MAPPER.readValue(propsRaw.toString(), Object.class) : null;
                featureObj.put("properties", props);
                // geometry 从字符串解析为 JSON 对象
                Object geomRaw = feature.get("geometry");
                Object geom = geomRaw != null ? MAPPER.readValue(geomRaw.toString(), Object.class) : null;
                featureObj.put("geometry", geom);
                writer.write(MAPPER.writeValueAsString(featureObj));
                first = false;
            }
            writer.flush();
            page++;
        } while (features.size() == pageSize);

        writer.write("]}");
        writer.flush();
    }

    private void exportCsv(String layerId, OutputStream outputStream) throws Exception {
        // UTF-8 BOM，解决 Excel 打开中文乱码
        outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));
        writer.println("id,properties,geometry");
        writer.flush();

        int page = 0;
        int pageSize = 500;
        List<Map<String, Object>> features;

        do {
            features = spatialMapper.findAsGeoJson(layerId, pageSize, page * pageSize);
            for (Map<String, Object> feature : features) {
                // id 前加 tab，强制 Excel 当文本处理，避免雪花 ID 末尾精度丢失
                String id = "\t" + feature.get("id");
                String properties = feature.get("properties") != null
                        ? MAPPER.writeValueAsString(feature.get("properties")).replace("\"", "\"\"")
                        : "";
                String geomJson = feature.get("geometry") != null ? feature.get("geometry").toString() : "";
                String escapedGeom = geomJson.replace("\"", "\"\"");
                writer.println(String.format("\"%s\",\"%s\",\"%s\"", id, properties, escapedGeom));
            }
            writer.flush();
            page++;
        } while (features.size() == pageSize);
    }

    @Getter
    @AllArgsConstructor
    public static class ExportPlan {
        private final String format;
        private final String contentType;
        private final String filename;
    }
}
