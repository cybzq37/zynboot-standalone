package com.zynboot.map.service;

import com.zynboot.kit.exception.BizException;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.response.source.SourceRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapSourceService {

    private final SourceRepository sourceRepository;
    private final LayerRepository layerRepository;
    private final LayerCacheVersionService layerCacheVersionService;

    public List<SourceRes> listByLayer(String layerId) {
        return sourceRepository.findByLayerId(layerId).stream().map(this::toRes).toList();
    }

    public SourceRes getById(String id) {
        SourceAggregate source = sourceRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("数据源"));
        return toRes(source);
    }

    @Transactional
    public void delete(String id) {
        SourceAggregate source = sourceRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("数据源"));
        String layerId = source.getLayerId();
        sourceRepository.delete(id);

        LayerAggregate layer = layerRepository.findById(layerId)
                .orElseThrow(() -> BizException.notFound("图层"));
        List<SourceAggregate> remaining = sourceRepository.findByLayerId(layerId);
        layer.getEntity().setSourceCount(remaining.size());
        layer.getEntity().setFeatureCount(remaining.stream()
                .map(SourceAggregate::getFeatureCount)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        layerRepository.update(layer);
        layerCacheVersionService.bumpVersion(layerId);
    }

    public SourceRes toRes(SourceAggregate source) {
        return SourceRes.builder()
                .id(source.getId())
                .layerId(source.getLayerId())
                .name(source.getEntity().getName())
                .type(source.getType())
                .format(source.getFormat())
                .sourceSrid(source.getEntity().getSourceSrid())
                .targetSrid(source.getEntity().getTargetSrid())
                .featureCount(source.getFeatureCount())
                .status(source.getStatus())
                .storageKey(source.getEntity().getStorageKey())
                .dataSourceId(source.getEntity().getDataSourceId())
                .externalTable(source.getEntity().getExternalTable())
                .createTime(source.getEntity().getCreateTime())
                .updateTime(source.getEntity().getUpdateTime())
                .build();
    }
}
