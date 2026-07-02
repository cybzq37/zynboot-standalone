package com.zynboot.map.service.datasource;

import com.zynboot.map.infrastructure.entity.MapDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理。
 * <p>
 * 启动时注册 ACTIVE 的 map_data_source 到连接池。
 * 运行时支持动态增删数据源。
 * <p>
 * 复用 zyn-kit 的连接管理思路，底层使用 HikariCP。
 */
@Slf4j
@Service
public class DynamicDataSourceService {

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * 获取或创建数据源。
     * 首次调用时创建连接池，后续复用缓存。
     */
    public DataSource getOrCreateDataSource(MapDataSource ds) {
        return dataSourceCache.computeIfAbsent(ds.getId(), id -> {
            log.info("Creating datasource: id={}, name={}, url={}", id, ds.getName(), ds.getUrl());
            return createHikariDataSource(ds);
        });
    }

    /**
     * 移除数据源（连接配置变更或删除时调用）。
     */
    public void removeDataSource(String dataSourceId) {
        DataSource removed = dataSourceCache.remove(dataSourceId);
        if (removed instanceof com.zaxxer.hikari.HikariDataSource hikari) {
            hikari.close();
            log.info("Datasource closed: {}", dataSourceId);
        }
    }

    /**
     * 测试连接是否可用。
     */
    public boolean testConnection(MapDataSource ds) {
        try {
            DataSource dataSource = getOrCreateDataSource(ds);
            try (var conn = dataSource.getConnection()) {
                if (ds.getTestQuery() != null && !ds.getTestQuery().isBlank()) {
                    try (var stmt = conn.createStatement()) {
                        stmt.execute(ds.getTestQuery());
                    }
                }
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.warn("Connection test failed: name={}, error={}", ds.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前缓存的数据源数量。
     */
    public int getActiveCount() {
        return dataSourceCache.size();
    }

    private DataSource createHikariDataSource(MapDataSource ds) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setPoolName("map-ds-" + ds.getName());
        config.setJdbcUrl(ds.getUrl());
        config.setUsername(ds.getUsername());
        config.setPassword(ds.getPassword() != null ? ds.getPassword() : "");
        config.setDriverClassName(ds.getDriverClass() != null ? ds.getDriverClass() : "org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        config.setValidationTimeout(5000);

        if (ds.getTestQuery() != null && !ds.getTestQuery().isBlank()) {
            config.setConnectionTestQuery(ds.getTestQuery());
        } else {
            config.setConnectionTestQuery("SELECT 1");
        }

        return new com.zaxxer.hikari.HikariDataSource(config);
    }
}
