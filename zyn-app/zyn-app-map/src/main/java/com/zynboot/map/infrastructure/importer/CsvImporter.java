package com.zynboot.map.infrastructure.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV 导入器：经纬度列 → 点要素。
 * CSV 第一行为表头，必须包含经纬度列（自动检测 lng/lon, longitude/latitude, x/y）。
 */
@Slf4j
@Component
public class CsvImporter implements VectorImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean supports(String format) {
        return "CSV".equalsIgnoreCase(format);
    }

    @Override
    public List<FeatureRecord> parse(InputStream input, String sourceSrid) throws Exception {
        List<FeatureRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return records;

            String[] headers = parseCsvLine(headerLine);
            int lngIdx = findColumn(headers, "lng", "longitude", "lon", "x");
            int latIdx = findColumn(headers, "lat", "latitude", "y");

            if (lngIdx < 0 || latIdx < 0) {
                throw new IllegalArgumentException("CSV 必须包含经纬度列（lng/lon/x 和 lat/latitude/y）");
            }

            String line;
            int rowNum = 1;
            List<String> errors = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                String[] values = parseCsvLine(line);
                if (values.length <= Math.max(lngIdx, latIdx)) {
                    errors.add("row[" + rowNum + "]: 列数不足");
                    continue;
                }

                try {
                    double lng = Double.parseDouble(values[lngIdx].trim());
                    double lat = Double.parseDouble(values[latIdx].trim());

                    if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
                        errors.add("row[" + rowNum + "]: 坐标越界 (" + lng + "," + lat + ")");
                        continue;
                    }

                    Map<String, Object> props = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != lngIdx && i != latIdx && i < values.length) {
                            props.put(headers[i].trim(), values[i].trim());
                        }
                    }

                    String propsJson = MAPPER.writeValueAsString(props);
                    String geomJson = String.format("{\"type\":\"Point\",\"coordinates\":[%s,%s]}", lng, lat);
                    records.add(new FeatureRecord(propsJson, geomJson));
                } catch (NumberFormatException e) {
                    errors.add("row[" + rowNum + "]: 数值解析失败 - " + e.getMessage());
                }
            }
            if (!errors.isEmpty()) {
                log.warn("CSV import warnings: {}", errors.size());
            }
        }
        log.info("CSV parsed: {} features", records.size());
        return records;
    }

    private int findColumn(String[] headers, String... names) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            for (String name : names) {
                if (h.equals(name)) return i;
            }
        }
        return -1;
    }

    private String[] parseCsvLine(String line) {
        // RFC 4180 CSV parser: handles quoted fields and doubled-quote escape sequence
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // RFC 4180 doubled-quote escape: append one literal quote
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
