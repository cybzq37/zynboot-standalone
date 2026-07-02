package com.zynboot.map.infrastructure.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.infra.geo.GeoFormatUtils;
import com.zynboot.infra.geo.shp.ShpReader;
import com.zynboot.infra.geo.shp.ShpReadResult;
import com.zynboot.infra.geo.shp.ShpFeatureData;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Shapefile 导入器：解析 SHP 文件，返回要素列表。
 */
@Slf4j
@Component
public class ShpImporter implements VectorImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean supports(String format) {
        return "SHP".equalsIgnoreCase(format);
    }

    @Override
    public List<FeatureRecord> parse(InputStream input, String sourceSrid) throws Exception {
        List<FeatureRecord> records = new ArrayList<>();

        // 写入临时文件（ShpReader 需要文件路径）
        Path tempFile = Files.createTempFile("zyn-shp-", ".shp");
        try {
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            ShpReadResult result = ShpReader.read(tempFile.toString());
            List<ShpFeatureData> features = result.features();

            for (ShpFeatureData feature : features) {
                Geometry geom = feature.geometry();
                if (geom == null) continue;

                String geomGeoJson = GeoFormatUtils.geometryToGeoJson(geom);

                Map<String, Object> props = new LinkedHashMap<>();
                Map<String, Object> attrs = feature.attributes();
                if (attrs != null) {
                    props.putAll(attrs);
                }

                String propsJson = MAPPER.writeValueAsString(props);
                records.add(new FeatureRecord(propsJson, geomGeoJson));
            }

            log.info("SHP parsed: {} features", records.size());
        } finally {
            Files.deleteIfExists(tempFile);
            // 清理同名的 .dbf, .shx, .prj 等关联文件
            String basePath = tempFile.toString().replace(".shp", "");
            Files.deleteIfExists(Path.of(basePath + ".dbf"));
            Files.deleteIfExists(Path.of(basePath + ".shx"));
            Files.deleteIfExists(Path.of(basePath + ".prj"));
        }
        return records;
    }
}
