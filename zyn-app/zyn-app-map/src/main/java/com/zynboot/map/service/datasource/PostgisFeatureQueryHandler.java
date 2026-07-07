package com.zynboot.map.service.datasource;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.JsonNode;
import com.zynboot.kit.util.JsonUtils;
import com.zynboot.map.infrastructure.entity.MapDataSource;
import com.zynboot.map.infrastructure.entity.MapLayerSource;
import com.zynboot.map.infrastructure.mapper.MapDataSourceMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * PostGIS 直查模式：通过 dynamic-datasource 切换到外部数据库查询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgisFeatureQueryHandler implements FeatureQueryHandler {

    private final MapLayerSourceMapper sourceMapper;
    private final MapDataSourceMapper dataSourceMapper;
    private final DynamicDataSourceService dataSourceService;

    @Override
    public boolean supports(String sourceType) {
        return "POSTGIS".equals(sourceType);
    }

    @Override
    public List<Map<String, Object>> list(String sourceId, String layerId, int limit, int offset) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return Collections.emptyList();

        MapDataSource ds = dataSourceMapper.selectById(source.getDataSourceId());
        if (ds == null) return Collections.emptyList();

        String schema = quoteIdentifier(source.getExternalSchema() != null ? source.getExternalSchema() : ds.getSchemaName());
        String table = quoteIdentifier(source.getExternalTable());
        String geomCol = quoteIdentifier(source.getExternalGeomCol() != null ? source.getExternalGeomCol() : "geom");
        String idCol = quoteIdentifier(source.getExternalIdCol() != null ? source.getExternalIdCol() : "gid");
        String propertiesExpr = buildPropertiesExpression(source);

        String sql = String.format(
                "SELECT %s AS id, %s AS properties, ST_AsGeoJSON(%s) AS geometry " +
                        "FROM %s.%s t LIMIT ? OFFSET ?",
                idCol, propertiesExpr, geomCol, schema, table);
        return executeOnDataSource(ds, sql, limit, offset);
    }

    @Override
    public List<Map<String, Object>> queryByBbox(String sourceId, String layerId,
                                                  double[] bbox, int limit, int offset) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return Collections.emptyList();

        MapDataSource ds = dataSourceMapper.selectById(source.getDataSourceId());
        if (ds == null) return Collections.emptyList();

        String schema = quoteIdentifier(source.getExternalSchema() != null ? source.getExternalSchema() : ds.getSchemaName());
        String table = quoteIdentifier(source.getExternalTable());
        String geomCol = quoteIdentifier(source.getExternalGeomCol() != null ? source.getExternalGeomCol() : "geom");
        String idCol = quoteIdentifier(source.getExternalIdCol() != null ? source.getExternalIdCol() : "gid");
        String propertiesExpr = buildPropertiesExpression(source);

        String sql = String.format(
                "SELECT %s AS id, %s AS properties, ST_AsGeoJSON(%s) AS geometry " +
                "FROM %s.%s t " +
                "WHERE %s && ST_MakeEnvelope(?, ?, ?, ?, 4326) " +
                "LIMIT ? OFFSET ?",
                idCol, propertiesExpr, geomCol, schema, table, geomCol);

        return executeOnDataSource(ds, sql, bbox[0], bbox[1], bbox[2], bbox[3], limit, offset);
    }

    @Override
    public List<Map<String, Object>> search(String sourceId, String layerId,
                                             String query, int limit, int offset) {
        // PostGIS 直查暂不支持全文搜索（除非外部表也装了 pg_search）
        return Collections.emptyList();
    }

    @Override
    public long count(String sourceId, String layerId) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return 0;

        MapDataSource ds = dataSourceMapper.selectById(source.getDataSourceId());
        if (ds == null) return 0;

        String schema = quoteIdentifier(source.getExternalSchema() != null ? source.getExternalSchema() : ds.getSchemaName());
        String table = quoteIdentifier(source.getExternalTable());

        String sql = String.format("SELECT COUNT(*) FROM %s.%s", schema, table);
        List<Map<String, Object>> result = executeOnDataSource(ds, sql);
        if (!result.isEmpty()) {
            Object count = result.get(0).values().iterator().next();
            return count instanceof Number ? ((Number) count).longValue() : 0;
        }
        return 0;
    }

    @Override
    public long countByBbox(String sourceId, String layerId, double[] bbox) {
        MapLayerSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDataSourceId() == null) return 0;

        MapDataSource ds = dataSourceMapper.selectById(source.getDataSourceId());
        if (ds == null) return 0;

        String schema = quoteIdentifier(source.getExternalSchema() != null ? source.getExternalSchema() : ds.getSchemaName());
        String table = quoteIdentifier(source.getExternalTable());
        String geomCol = quoteIdentifier(source.getExternalGeomCol() != null ? source.getExternalGeomCol() : "geom");

        String sql = String.format(
                "SELECT COUNT(*) FROM %s.%s WHERE %s && ST_MakeEnvelope(?, ?, ?, ?, 4326)",
                schema, table, geomCol);

        List<Map<String, Object>> result = executeOnDataSource(ds, sql, bbox[0], bbox[1], bbox[2], bbox[3]);
        if (!result.isEmpty()) {
            Object count = result.get(0).values().iterator().next();
            return count instanceof Number ? ((Number) count).longValue() : 0;
        }
        return 0;
    }

    private List<Map<String, Object>> executeOnDataSource(MapDataSource ds, String sql, Object... params) {
        try {
            DataSource dataSource = dataSourceService.getOrCreateDataSource(ds);
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return mapResultSet(rs);
                }
            }
        } catch (Exception e) {
            log.error("PostGIS query failed: dataSource={}, sql={}", ds.getName(), sql, e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建 properties 字段的 SQL 表达式。
     * <p>
     * 规则：
     * <ul>
     *   <li>若 source.fieldMapping 配置了字段白名单，按白名单用 jsonb_build_object 精确构造</li>
     *   <li>否则用 to_jsonb(t) 返回数据库行所有字段（不做排除）</li>
     * </ul>
     * <p>
     * fieldMapping 支持两种 JSON 格式：
     * <ul>
     *   <li>对象（别名映射）：{"别名": "外部列名"}，例：{"名称": "aoi_name"}</li>
     *   <li>数组（直接列名）：["aoi_name", "address"]</li>
     * </ul>
     */
    private String buildPropertiesExpression(MapLayerSource source) {
        String fieldMapping = source.getFieldMapping();
        if (fieldMapping != null && !fieldMapping.isBlank()) {
            try {
                JsonNode node = JsonUtils.mapper().readTree(fieldMapping);
                String whiteListExpr = buildWhiteListExpression(node);
                if (whiteListExpr != null) {
                    return whiteListExpr;
                }
            } catch (Exception e) {
                log.warn("解析 field_mapping 失败，回退到默认模式: {}", fieldMapping, e);
            }
        }
        // 默认：按数据库原样返回全部字段
        return "to_jsonb(t)";
    }

    /**
     * 根据白名单配置构造 jsonb_build_object(...) 表达式。
     *
     * @return SQL 表达式；若配置无效返回 null
     */
    private String buildWhiteListExpression(JsonNode node) {
        List<String[]> pairs = new ArrayList<>(); // [alias, column]
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String alias = entry.getKey();
                String column = entry.getValue().asText();
                if (!alias.isBlank() && !column.isBlank()) {
                    pairs.add(new String[]{alias, column});
                }
            });
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                String column = item.asText();
                if (!column.isBlank()) {
                    pairs.add(new String[]{column, column});
                }
            }
        }
        if (pairs.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("jsonb_build_object(");
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0) sb.append(", ");
            String alias = pairs.get(i)[0].replace("'", "''");
            String column = quoteIdentifier(pairs.get(i)[1]);
            sb.append("'").append(alias).append("', t.").append(column);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 引用 PostgreSQL 标识符，防止 SQL 注入。
     * 将标识符用双引号包裹，并对内部的引号做转义。
     */
    private static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("SQL 标识符不能为空");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private List<Map<String, Object>> mapResultSet(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<Map<String, Object>> results = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            Object rawProperties = null;
            Object rawGeometry = null;
            for (int i = 1; i <= colCount; i++) {
                String label = meta.getColumnLabel(i);
                Object value = unwrapPgObject(rs.getObject(i));
                switch (label) {
                    case "properties" -> rawProperties = value;
                    case "geometry" -> rawGeometry = value;
                    default -> row.put(label, value);
                }
            }
            mergeProperties(row, rawProperties);
            row.put("geometry", parseGeometry(rawGeometry));
            results.add(row);
        }
        return results;
    }

    /**
     * 将 properties JSON 字符串平铺到外层 row。
     * <p>
     * 若 properties 为对象，其键值对直接放入 row；遇到与 id、geometry 同名的键时跳过，
     * 避免覆盖 PostgreSQL 查询返回的主键和几何字段。
     */
    private static void mergeProperties(Map<String, Object> row, Object rawProperties) {
        if (!(rawProperties instanceof String propertiesText) || propertiesText.isBlank()) {
            return;
        }
        try {
            JsonNode node = JsonUtils.mapper().readTree(propertiesText);
            if (!node.isObject()) {
                return;
            }
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if ("id".equals(key) || "geometry".equals(key)) {
                    return;
                }
                row.put(key, JsonUtils.mapper().convertValue(entry.getValue(), Object.class));
            });
        } catch (Exception e) {
            log.warn("解析 properties 失败: {}", propertiesText, e);
        }
    }

    /**
     * 将 GeoJSON 字符串解析为 JSON 对象。
     */
    private static Object parseGeometry(Object rawGeometry) {
        if (!(rawGeometry instanceof String geometryText) || geometryText.isBlank()) {
            return rawGeometry;
        }
        try {
            return JsonUtils.mapper().readTree(geometryText);
        } catch (Exception e) {
            log.warn("解析 geometry 失败: {}", geometryText, e);
            return geometryText;
        }
    }

    /**
     * 解包 PostgreSQL 特有类型（如 PGobject）。
     * <p>
     * JDBC 驱动对 JSONB/JSON/HSTORE 等列默认返回 PGobject，
     * 其 toString() 形如 {@code <type:value>};直接序列化会输出 {type,value,null} 结构。
     * 这里取其 value 字段，返回纯字符串。
     */
    private static Object unwrapPgObject(Object value) {
        if (value == null) {
            return null;
        }
        String className = value.getClass().getName();
        if (className.startsWith("org.postgresql.util.") && "org.postgresql.util.PGobject".equals(className)) {
            // 反射取 value 字段，避免直接依赖 postgresql 驱动类
            try {
                java.lang.reflect.Field valueField = value.getClass().getDeclaredField("value");
                valueField.setAccessible(true);
                return valueField.get(value);
            } catch (ReflectiveOperationException e) {
                // 回退：PGobject.toString() 对 JSONB 返回纯 JSON 字符串
                return value.toString();
            }
        }
        return value;
    }
}
