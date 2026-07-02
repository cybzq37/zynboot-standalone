package com.zynboot.map.service;

import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.command.datasource.DataSourceSaveCmd;
import com.zynboot.map.infrastructure.entity.MapDataSource;
import com.zynboot.map.infrastructure.mapper.MapDataSourceMapper;
import com.zynboot.map.response.datasource.ConnectionTestRes;
import com.zynboot.map.response.datasource.DataSourceRes;
import com.zynboot.map.service.datasource.DynamicDataSourceService;
import com.zynboot.map.service.datasource.EsClientManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapDataSourceService {

    private final MapDataSourceMapper dataSourceMapper;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final EsClientManager esClientManager;

    public List<DataSourceRes> list() {
        return dataSourceMapper.selectList(null).stream().map(this::toRes).toList();
    }

    public DataSourceRes getById(String id) {
        return toRes(requireDataSource(id));
    }

    @Transactional
    public DataSourceRes create(DataSourceSaveCmd cmd) {
        MapDataSource dataSource = new MapDataSource();
        dataSource.setId(IdUtils.uuid());
        apply(dataSource, cmd, null);
        dataSourceMapper.insert(dataSource);
        return toRes(dataSourceMapper.selectById(dataSource.getId()));
    }

    @Transactional
    public DataSourceRes update(String id, DataSourceSaveCmd cmd) {
        MapDataSource existing = requireDataSource(id);
        apply(existing, cmd, existing);
        dataSourceMapper.updateById(existing);
        evictClients(existing);
        return toRes(dataSourceMapper.selectById(id));
    }

    @Transactional
    public void delete(String id) {
        MapDataSource existing = requireDataSource(id);
        evictClients(existing);
        dataSourceMapper.deleteById(id);
    }

    public ConnectionTestRes testExisting(String id) {
        return test(requireDataSource(id));
    }

    public ConnectionTestRes testNew(DataSourceSaveCmd cmd) {
        MapDataSource dataSource = new MapDataSource();
        apply(dataSource, cmd, null);
        return test(dataSource);
    }

    private ConnectionTestRes test(MapDataSource dataSource) {
        boolean ok = isElastic(dataSource) ? esClientManager.testConnection(dataSource) : dynamicDataSourceService.testConnection(dataSource);
        return ConnectionTestRes.builder()
                .status(ok ? "CONNECTED" : "FAILED")
                .message(ok ? "连接成功" : "连接失败")
                .build();
    }

    private void evictClients(MapDataSource dataSource) {
        dynamicDataSourceService.removeDataSource(dataSource.getId());
        esClientManager.removeEsClient(dataSource.getId());
    }

    private boolean isElastic(MapDataSource dataSource) {
        return "ELASTICSEARCH".equalsIgnoreCase(dataSource.getType());
    }

    private MapDataSource requireDataSource(String id) {
        MapDataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) throw BizException.notFound("数据源");
        return ds;
    }

    private void apply(MapDataSource target, DataSourceSaveCmd cmd, MapDataSource existing) {
        target.setName(cmd.getName());
        target.setType(cmd.getType());
        target.setUrl(cmd.getUrl());
        target.setUsername(cmd.getUsername());
        target.setPassword(cmd.getPassword() != null && !cmd.getPassword().isBlank()
                ? cmd.getPassword()
                : existing != null ? existing.getPassword() : null);
        target.setSchemaName(cmd.getSchemaName());
        target.setDriverClass(cmd.getDriverClass());
        target.setTestQuery(cmd.getTestQuery());
        target.setStatus(cmd.getStatus() != null ? cmd.getStatus() : "ACTIVE");
    }

    private DataSourceRes toRes(MapDataSource dataSource) {
        return DataSourceRes.builder()
                .id(dataSource.getId())
                .name(dataSource.getName())
                .type(dataSource.getType())
                .url(dataSource.getUrl())
                .username(dataSource.getUsername())
                .schemaName(dataSource.getSchemaName())
                .driverClass(dataSource.getDriverClass())
                .testQuery(dataSource.getTestQuery())
                .status(dataSource.getStatus())
                .createTime(dataSource.getCreateTime())
                .updateTime(dataSource.getUpdateTime())
                .build();
    }
}
