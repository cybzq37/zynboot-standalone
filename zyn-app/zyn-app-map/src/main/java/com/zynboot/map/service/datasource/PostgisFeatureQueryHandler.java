package com.zynboot.map.service.datasource;

import com.baomidou.dynamic.datasource.annotation.DS;
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

        String sql = String.format(
                "SELECT %s AS id, row_to_json(t) AS properties, ST_AsGeoJSON(%s) AS geometry " +
                        "FROM %s.%s t LIMIT ? OFFSET ?",
                idCol, geomCol, schema, table);
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

        String sql = String.format(
                "SELECT %s AS id, row_to_json(t) AS properties, ST_AsGeoJSON(%s) AS geometry " +
                "FROM %s.%s t " +
                "WHERE %s && ST_MakeEnvelope(?, ?, ?, ?, 4326) " +
                "LIMIT ? OFFSET ?",
                idCol, geomCol, schema, table, geomCol);

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
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            results.add(row);
        }
        return results;
    }
}
