package com.zynboot.map.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapLayerVersion;
import com.zynboot.map.infrastructure.mapper.MapLayerVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private final LayerRepository layerRepository;
    private final SourceRepository sourceRepository;
    private final MapLayerVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public MapLayerVersion createSnapshot(String layerId, String type, String name) {
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));

        List<SourceAggregate> sources = sourceRepository.findByLayerId(layerId);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("featureCount", layer.getFeatureCount());
        snapshot.put("sourceCount", layer.getSourceCount());
        snapshot.put("sources", sources.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getEntity().getName());
            m.put("type", s.getType());
            m.put("featureCount", s.getFeatureCount());
            return m;
        }).toList());

        Integer maxVersion = versionMapper.selectList(
                new LambdaQueryWrapper<MapLayerVersion>()
                        .eq(MapLayerVersion::getLayerId, layerId)
                        .orderByDesc(MapLayerVersion::getVersion)
                        .last("LIMIT 1"))
                .stream().findFirst().map(MapLayerVersion::getVersion).orElse(0);

        MapLayerVersion version = new MapLayerVersion();
        version.setId(IdUtils.uuid());
        version.setLayerId(layerId);
        version.setVersion(maxVersion + 1);
        version.setName(name);
        version.setType(type);
        try {
            version.setSourceSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            version.setSourceSnapshot("{}");
        }
        version.setFeatureCount(layer.getFeatureCount());
        version.setSourceCount(layer.getSourceCount());
        versionMapper.insert(version);

        log.info("Version snapshot created: layerId={}, version={}", layerId, version.getVersion());
        return version;
    }

    /**
     * 回滚到指定版本。
     * 只删除目标版本快照中不存在的 source（即快照后新增的 source），保留快照中的。
     */
    @Transactional
    public void rollback(String layerId, int targetVersion) {
        MapLayerVersion target = versionMapper.selectOne(
                new LambdaQueryWrapper<MapLayerVersion>()
                        .eq(MapLayerVersion::getLayerId, layerId)
                        .eq(MapLayerVersion::getVersion, targetVersion));
        if (target == null) throw new IllegalArgumentException("版本不存在: " + targetVersion);

        // 解析快照中的 source ID 列表
        Set<String> snapshotSourceIds = parseSnapshotSourceIds(target.getSourceSnapshot());

        // 获取当前所有 source
        List<SourceAggregate> currentSources = sourceRepository.findByLayerId(layerId);
        Set<String> currentSourceIds = currentSources.stream()
                .map(SourceAggregate::getId)
                .collect(Collectors.toSet());

        // 找出快照后新增的 source（当前有但快照中没有）
        Set<String> toDelete = new HashSet<>(currentSourceIds);
        toDelete.removeAll(snapshotSourceIds);

        for (String sourceId : toDelete) {
            sourceRepository.delete(sourceId);
            log.info("Rollback: deleted source {}", sourceId);
        }

        // 删除该版本之后的所有版本记录
        versionMapper.delete(
                new LambdaQueryWrapper<MapLayerVersion>()
                        .eq(MapLayerVersion::getLayerId, layerId)
                        .gt(MapLayerVersion::getVersion, targetVersion));

        // 重新计算图层统计
        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> new IllegalArgumentException("图层不存在: " + layerId));
        List<SourceAggregate> remaining = sourceRepository.findByLayerId(layerId);
        int totalFeatures = remaining.stream().mapToInt(SourceAggregate::getFeatureCount).sum();
        layer.getEntity().setFeatureCount(totalFeatures);
        layer.getEntity().setSourceCount(remaining.size());
        layerRepository.update(layer);

        log.info("Rolled back: layerId={}, to version={}, deleted {} sources", layerId, targetVersion, toDelete.size());
    }

    private Set<String> parseSnapshotSourceIds(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank() || "{}".equals(snapshotJson.trim())) {
            return Collections.emptySet();
        }
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, new TypeReference<>() {});
            Object sourcesObj = snapshot.get("sources");
            if (sourcesObj instanceof List<?> sources) {
                return sources.stream()
                        .filter(Map.class::isInstance)
                        .map(s -> (Map<String, Object>) s)
                        .map(m -> (String) m.get("id"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("Failed to parse snapshot: {}", e.getMessage());
        }
        return Collections.emptySet();
    }
}
