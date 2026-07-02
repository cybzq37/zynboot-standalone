package com.zynboot.map.infrastructure.importer;

import java.io.InputStream;
import java.util.List;

/**
 * 矢量数据导入器接口。
 */
public interface VectorImporter {

    /**
     * 检测是否能处理该格式。
     */
    boolean supports(String format);

    /**
     * 解析文件，返回要素列表。
     * 每个 FeatureRecord 包含 properties (JSON) 和 geometry (GeoJSON)。
     */
    List<FeatureRecord> parse(InputStream input, String sourceSrid) throws Exception;

    record FeatureRecord(String propertiesJson, String geometryGeoJson) {}
}
