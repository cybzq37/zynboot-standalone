package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapAsyncTask;
import com.zynboot.map.infrastructure.entity.MapLayerVersion;
import com.zynboot.map.infrastructure.entity.MapOperationLog;
import com.zynboot.map.infrastructure.entity.MapSourceProxy;
import com.zynboot.map.infrastructure.entity.MapSourceRaster;
import com.zynboot.map.infrastructure.entity.MapSourceTile;
import com.zynboot.map.infrastructure.mapper.MapLayerVersionMapper;
import com.zynboot.map.infrastructure.mapper.MapOperationLogMapper;
import com.zynboot.map.infrastructure.mapper.MapSourceProxyMapper;
import com.zynboot.map.infrastructure.mapper.MapSourceRasterMapper;
import com.zynboot.map.infrastructure.mapper.MapSourceTileMapper;
import com.zynboot.map.response.audit.AuditLogRes;
import com.zynboot.map.response.source.ProxyHealthRes;
import com.zynboot.map.response.source.RasterMetaRes;
import com.zynboot.map.response.source.SourceTileRes;
import com.zynboot.map.response.version.LayerVersionRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapQueryFacadeService {

    private final MapOperationLogMapper operationLogMapper;
    private final MapLayerVersionMapper layerVersionMapper;
    private final MapSourceProxyMapper sourceProxyMapper;
    private final MapSourceRasterMapper sourceRasterMapper;
    private final MapSourceTileMapper sourceTileMapper;
    private final ProxyHealthCheckService proxyHealthCheckService;
    private final VersionService versionService;
    private final SourceRepository sourceRepository;
    private final LayerCacheVersionService layerCacheVersionService;

    public List<AuditLogRes> listAudit(String targetType, String targetId, String action, String operatorId, int pageNum, int pageSize) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<MapOperationLog>()
                        .eq(targetType != null && !targetType.isBlank(), MapOperationLog::getTargetType, targetType)
                        .eq(targetId != null && !targetId.isBlank(), MapOperationLog::getTargetId, targetId)
                        .eq(action != null && !action.isBlank(), MapOperationLog::getAction, action)
                        .eq(operatorId != null && !operatorId.isBlank(), MapOperationLog::getOperatorId, operatorId)
                        .orderByDesc(MapOperationLog::getCreateTime)
                        .last("LIMIT " + pageSize + " OFFSET " + (pageNum - 1) * pageSize))
                .stream().map(this::toAuditRes).toList();
    }

    public List<AuditLogRes> getAuditByTarget(String targetType, String targetId) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<MapOperationLog>()
                        .eq(MapOperationLog::getTargetType, targetType)
                        .eq(MapOperationLog::getTargetId, targetId)
                        .orderByDesc(MapOperationLog::getCreateTime))
                .stream().map(this::toAuditRes).toList();
    }

    public List<LayerVersionRes> listVersions(String layerId) {
        return layerVersionMapper.selectList(new LambdaQueryWrapper<MapLayerVersion>()
                        .eq(MapLayerVersion::getLayerId, layerId)
                        .orderByDesc(MapLayerVersion::getVersion))
                .stream().map(this::toLayerVersionRes).toList();
    }

    public LayerVersionRes getVersion(String layerId, Integer version) {
        MapLayerVersion entity = layerVersionMapper.selectOne(new LambdaQueryWrapper<MapLayerVersion>()
                .eq(MapLayerVersion::getLayerId, layerId)
                .eq(MapLayerVersion::getVersion, version));
        if (entity == null) throw BizException.notFound("版本");
        return toLayerVersionRes(entity);
    }

    public LayerVersionRes createVersion(String layerId) {
        return toLayerVersionRes(versionService.createSnapshot(layerId, "MANUAL", "手动快照"));
    }

    public void rollback(String layerId, Integer version) {
        versionService.rollback(layerId, version);
        layerCacheVersionService.bumpVersion(layerId);
    }

    public ProxyHealthRes getProxyHealth(String sourceId) {
        return toProxyHealthRes(requireProxy(sourceId));
    }

    public ProxyHealthRes triggerProxyCheck(String sourceId) {
        MapSourceProxy proxy = requireProxy(sourceId);
        proxyHealthCheckService.checkOne(proxy);
        return toProxyHealthRes(requireProxy(sourceId));
    }

    public RasterMetaRes getRasterMeta(String sourceId) {
        MapSourceRaster raster = sourceRasterMapper.selectOne(
                new LambdaQueryWrapper<MapSourceRaster>().eq(MapSourceRaster::getSourceId, sourceId));
        if (raster == null) throw BizException.notFound("栅格元数据");
        return RasterMetaRes.builder()
                .sourceId(raster.getSourceId())
                .path(raster.getPath())
                .width(raster.getWidth())
                .height(raster.getHeight())
                .bands(raster.getBands())
                .dataType(raster.getDataType())
                .nodata(raster.getNodata())
                .pixelSizeX(raster.getPixelSizeX())
                .pixelSizeY(raster.getPixelSizeY())
                .compressedBytes(raster.getCompressedBytes())
                .uncompressedBytes(raster.getUncompressedBytes())
                .build();
    }

    public SourceTileRes getTileStatus(String sourceId) {
        MapSourceTile tile = sourceTileMapper.selectOne(
                new LambdaQueryWrapper<MapSourceTile>().eq(MapSourceTile::getSourceId, sourceId));
        if (tile == null) throw BizException.notFound("瓦片信息");
        return SourceTileRes.builder()
                .sourceId(tile.getSourceId())
                .status(tile.getStatus())
                .path(tile.getPath())
                .minZoom(tile.getMinZoom())
                .maxZoom(tile.getMaxZoom())
                .format(tile.getFormat())
                .tileSize(tile.getTileSize())
                .tileCount(tile.getTileCount())
                .progress(tile.getProgress())
                .build();
    }

    public SourceAggregate requireSource(String sourceId) {
        return sourceRepository.findById(sourceId)
                .orElseThrow(() -> BizException.notFound("数据源"));
    }

    private MapSourceProxy requireProxy(String sourceId) {
        MapSourceProxy proxy = sourceProxyMapper.selectOne(
                new LambdaQueryWrapper<MapSourceProxy>().eq(MapSourceProxy::getSourceId, sourceId));
        if (proxy == null) throw BizException.notFound("代理配置");
        return proxy;
    }

    private AuditLogRes toAuditRes(MapOperationLog log) {
        return AuditLogRes.builder()
                .id(log.getId())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .action(log.getAction())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .detailJson(log.getDetailJson())
                .ip(log.getIp())
                .createTime(log.getCreateTime())
                .build();
    }

    private LayerVersionRes toLayerVersionRes(MapLayerVersion version) {
        return LayerVersionRes.builder()
                .id(version.getId())
                .layerId(version.getLayerId())
                .version(version.getVersion())
                .name(version.getName())
                .type(version.getType())
                .sourceSnapshot(version.getSourceSnapshot())
                .featureCount(version.getFeatureCount())
                .sourceCount(version.getSourceCount())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .build();
    }

    private ProxyHealthRes toProxyHealthRes(MapSourceProxy proxy) {
        return ProxyHealthRes.builder()
                .sourceId(proxy.getSourceId())
                .url(proxy.getUrl())
                .authType(proxy.getAuthType())
                .cacheTtl(proxy.getCacheTtl())
                .healthStatus(proxy.getHealthStatus())
                .healthMessage(proxy.getHealthMessage())
                .lastCheckAt(proxy.getLastCheckAt())
                .failCount(proxy.getFailCount())
                .build();
    }
}
