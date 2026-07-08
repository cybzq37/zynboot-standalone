package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.DateUtils;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.kit.util.JsonUtils;
import com.zynboot.map.command.feature.FeatureSaveCmd;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapLayerFeature;
import com.zynboot.map.infrastructure.entity.MapLayerField;
import com.zynboot.map.infrastructure.mapper.MapLayerFieldMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerFeatureMapper;
import com.zynboot.map.infrastructure.mapper.MapSpatialMapper;
import com.zynboot.map.response.feature.FeaturePageRes;
import com.zynboot.map.response.feature.FeatureRes;
import com.zynboot.map.service.datasource.FeatureQuery;
import com.zynboot.map.service.datasource.FeatureQueryParser;
import com.zynboot.map.service.datasource.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MapLayerFeatureService {

    private final MapLayerFeatureMapper featureMapper;
    private final MapSpatialMapper spatialMapper;
    private final FeatureService queryService;
    private final LayerRepository layerRepository;
    private final SourceRepository sourceRepository;
    private final LayerCacheVersionService layerCacheVersionService;
    private final MapLayerFieldMapper layerFieldMapper;
    private final FeatureQueryParser queryParser;

    /** 单次最多返回的要素数量上限 */
    private static final int MAX_FEATURE_LIMIT = 1000;

    public FeaturePageRes listByLayer(String layerId, String bbox, String query) {
        int limit = MAX_FEATURE_LIMIT;
        int offset = 0;
        double[] bboxArr = bbox != null && !bbox.isBlank() ? parseBbox(bbox) : null;
        FeatureQuery featureQuery = queryParser.parse(layerId, query);
        FeatureService.FeatureQueryResult result = queryService.queryWithFilter(
                layerId, null, featureQuery, bboxArr, limit, offset);
        return FeaturePageRes.builder()
                .items(result.items())
                .total(result.total())
                .pageNum(1)
                .pageSize(limit)
                .querySourceId(result.sourceId())
                .querySourceType(result.sourceType())
                .build();
    }

    public FeaturePageRes page(String layerId, int pageNum, int pageSize, String query) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        FeatureQuery featureQuery = queryParser.parse(layerId, query);
        FeatureService.FeatureQueryResult result = queryService.queryWithFilter(
                layerId, null, featureQuery, null, pageSize, offset);
        return FeaturePageRes.builder()
                .items(result.items())
                .total(result.total())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .querySourceId(result.sourceId())
                .querySourceType(result.sourceType())
                .build();
    }

    public FeatureRes getById(Long id) {
        MapLayerFeature feature = featureMapper.selectViewById(id);
        if (feature == null) {
            throw BizException.notFound("要素");
        }
        return toRes(feature);
    }

    public List<Map<String, Object>> listAsGeoJson(String layerId, String sourceId, int pageNum, int pageSize) {
        requireLayer(layerId);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        if (sourceId != null && !sourceId.isBlank()) {
            return spatialMapper.findAsGeoJsonBySource(layerId, sourceId, pageSize, offset);
        }
        return spatialMapper.findAsGeoJson(layerId, pageSize, offset);
    }

    public List<Map<String, Object>> cluster(String layerId, int k, String bbox) {
        requireLayer(layerId);
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = parseBboxParts(bbox);
            return spatialMapper.clusterWithBbox(layerId, k, parts[0], parts[1], parts[2], parts[3]);
        }
        return spatialMapper.cluster(layerId, k);
    }

    public List<Map<String, Object>> searchBm25(String layerId, String query, String bbox, int pageNum, int pageSize) {
        requireLayer(layerId);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = parseBboxParts(bbox);
            return spatialMapper.searchBm25WithBbox(layerId, query, parts[0], parts[1], parts[2], parts[3], pageSize, offset);
        }
        return spatialMapper.searchBm25(layerId, query, pageSize, offset);
    }

    @Transactional
    public FeatureRes create(String layerId, FeatureSaveCmd cmd) {
        LayerAggregate layer = requireLayer(layerId);
        // 一图层一源：自动取图层绑定的数据源 ID，未绑定则为 NULL（手动新增要素）
        SourceAggregate source = sourceRepository.findByLayerId(layerId).stream().findFirst().orElse(null);
        assertWritableSource(source);
        assertGeometryType(cmd.getGeometry(), layer.getGeometryType());
        // 填充默认值并校验必填字段
        JsonNode properties = applyDefaultsAndValidate(layerId, cmd.getProperties());
        long id = IdUtils.snowflakeId();
        featureMapper.insertWithGeometry(
                id,
                layerId,
                source == null ? null : source.getId(),
                JsonUtils.toJson(properties),
                JsonUtils.toJson(cmd.getGeometry()));
        layer.incrementFeatureCount(1);
        layerRepository.update(layer);
        layerCacheVersionService.bumpVersion(layerId);
        return getById(id);
    }

    @Transactional
    public FeatureRes update(Long id, FeatureSaveCmd cmd) {
        MapLayerFeature existing = featureMapper.selectViewById(id);
        if (existing == null) {
            throw BizException.notFound("要素");
        }
        LayerAggregate layer = requireLayer(existing.getLayerId());
        // sourceId 不允许通过接口修改，保持原值；校验图层 source 是否可写
        SourceAggregate source = sourceRepository.findByLayerId(existing.getLayerId()).stream().findFirst().orElse(null);
        assertWritableSource(source);
        assertGeometryType(cmd.getGeometry(), layer.getGeometryType());
        assertRequiredFields(existing.getLayerId(), cmd.getProperties());
        featureMapper.updateWithGeometry(
                id,
                existing.getSourceId(),
                JsonUtils.toJson(cmd.getProperties()),
                JsonUtils.toJson(cmd.getGeometry()));
        layerCacheVersionService.bumpVersion(existing.getLayerId());
        return getById(id);
    }

    /**
     * 校验传入的 geometry 类型与图层预设的 geometryType 是否一致。
     * 支持图层 geometryType 为 MULTI* 时接受对应的单值类型（如 POLYGON→Polygon、MULTIPOLYGON→MultiPolygon 均可）。
     */
    private void assertGeometryType(JsonNode geometry, String layerGeometryType) {
        if (geometry == null || geometry.isMissingNode() || geometry.isNull()) {
            return;
        }
        JsonNode typeNode = geometry.get("type");
        if (typeNode == null || typeNode.isNull()) {
            throw BizException.badRequest("geometry 缺少 type 字段");
        }
        String inputType = typeNode.asText().toUpperCase();
        if (inputType.equals("GEOMETRYCOLLECTION")) {
            // GEOMETRYCOLLECTION 只允许图层 geometryType = GEOMETRYCOLLECTION
            if (!"GEOMETRYCOLLECTION".equals(layerGeometryType)) {
                throw BizException.badRequest(
                        "几何类型不匹配：图层要求 " + layerGeometryType + "，传入 GEOMETRYCOLLECTION");
            }
            return;
        }
        String normalizedLayer = layerGeometryType == null ? "" : layerGeometryType.toUpperCase();
        String normalizedInput = inputType;
        if (normalizedLayer.startsWith("MULTI") && !normalizedInput.startsWith("MULTI")) {
            // MULTI* 图层允许单值类型：如 POLYGON/MULTIPOLYGON 图层均接受 Polygon
            normalizedInput = "MULTI" + normalizedInput;
        }
        if (!normalizedInput.equals(normalizedLayer)) {
            throw BizException.badRequest(
                    "几何类型不匹配：图层要求 " + layerGeometryType + "，传入 " + typeNode.asText());
        }
    }

    /**
     * 校验图层绑定的数据源是否支持通过本接口写入要素。
     * 仅允许无数据源（手动新增）或 FILE 类型；外部数据源（POSTGIS/ES/WMS 等）不允许写入。
     */
    private void assertWritableSource(SourceAggregate source) {
        if (source == null) {
            return;
        }
        String type = source.getType();
        if (!"LOCAL".equals(type)) {
            throw BizException.badRequest("该图层为 " + type + " 外部数据源，不支持通过本接口创建/修改要素");
        }
    }

    /**
     * 填充默认值并校验必填字段（create 场景）。
     * <p>规则：
     * <ul>
     *   <li>properties 中缺失的字段，若字段定义有 default_value 则自动填充</li>
     *   <li>填充后仍缺失的 required 字段，抛 400</li>
     *   <li>已存在的字段值会做类型格式校验（目前仅 DATE 类型）</li>
     * </ul>
     */
    private JsonNode applyDefaultsAndValidate(String layerId, JsonNode properties) {
        List<MapLayerField> fields = layerFieldMapper.selectList(
                new LambdaQueryWrapper<MapLayerField>().eq(MapLayerField::getLayerId, layerId));
        if (fields.isEmpty()) {
            return properties;
        }
        ObjectNode node = properties != null && properties.isObject()
                ? (ObjectNode) properties
                : JsonUtils.mapper().createObjectNode();
        for (MapLayerField field : fields) {
            JsonNode value = node.get(field.getName());
            boolean missing = value == null || value.isNull();
            if (missing) {
                // 必填字段不填充默认值，强制由用户传入；非必填字段才允许用 default_value 兜底
                if (!Boolean.TRUE.equals(field.getRequired())) {
                    String defaultValue = field.getDefaultValue();
                    if (defaultValue != null && !defaultValue.isBlank()) {
                        node.put(field.getName(), defaultValue);
                    }
                }
                continue;
            }
            // 已存在的字段值做类型格式校验
            assertFieldType(field, value);
        }
        // 必填校验（填充默认值后仍可能缺失）
        for (MapLayerField field : fields) {
            if (Boolean.TRUE.equals(field.getRequired())) {
                JsonNode value = node.get(field.getName());
                if (value == null || value.isNull()) {
                    throw BizException.badRequest("缺少必填字段: " + field.getName());
                }
            }
        }
        return node;
    }

    /**
     * 校验必填字段及字段类型（update 场景，不填充默认值）。
     */
    private void assertRequiredFields(String layerId, JsonNode properties) {
        List<MapLayerField> fields = layerFieldMapper.selectList(
                new LambdaQueryWrapper<MapLayerField>().eq(MapLayerField::getLayerId, layerId));
        for (MapLayerField field : fields) {
            JsonNode value = properties == null ? null : properties.get(field.getName());
            if (value != null && !value.isNull()) {
                assertFieldType(field, value);
            }
            if (Boolean.TRUE.equals(field.getRequired())) {
                if (value == null || value.isNull()) {
                    throw BizException.badRequest("缺少必填字段: " + field.getName());
                }
            }
        }
    }

    /**
     * 校验字段值格式是否符合声明的类型（目前仅 DATE 类型做格式校验）。
     * 复用 DateUtils 支持的多种日期格式（ISO、epoch、yyyy-MM-dd、yyyyMMdd、yyyy年MM月dd日 等）。
     */
    private void assertFieldType(MapLayerField field, JsonNode value) {
        String type = field.getType();
        if (type == null) {
            return;
        }
        if ("DATE".equals(type.toUpperCase())) {
            String text = value.isTextual() ? value.asText() : value.toString();
            if (text != null && !text.isBlank() && DateUtils.parseDate(text) == null) {
                throw BizException.badRequest("字段 " + field.getName() + " 的日期格式无法解析: " + text);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        MapLayerFeature existing = featureMapper.selectViewById(id);
        if (existing == null) {
            return;
        }
        featureMapper.deleteByIdValue(id);
        layerRepository.findById(existing.getLayerId()).ifPresent(layer -> {
            int current = layer.getFeatureCount() != null ? layer.getFeatureCount() : 0;
            layer.getEntity().setFeatureCount(Math.max(0, current - 1));
            layerRepository.update(layer);
        });
        layerCacheVersionService.bumpVersion(existing.getLayerId());
    }

    private LayerAggregate requireLayer(String layerId) {
        return layerRepository.findById(layerId)
                .orElseThrow(() -> BizException.notFound("图层"));
    }

    private double[] parseBbox(String bbox) {
        String[] parts = parseBboxParts(bbox);
        return new double[] {
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim())
        };
    }

    private String[] parseBboxParts(String bbox) {
        String[] parts = bbox.split(",");
        if (parts.length != 4) {
            throw BizException.badRequest("bbox 格式应为 minX,minY,maxX,maxY");
        }
        return parts;
    }

    private FeatureRes toRes(MapLayerFeature feature) {
        return FeatureRes.builder()
                .id(feature.getId())
                .layerId(feature.getLayerId())
                .sourceId(feature.getSourceId())
                .properties(feature.getProperties())
                .geometry(feature.getGeometry())
                .center(feature.getCenter())
                .build();
    }
}
