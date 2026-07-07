package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.map.command.layer.LayerSaveCmd;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.handler.query.LayerQueryHandler;
import com.zynboot.map.infrastructure.entity.MapLayerField;
import com.zynboot.map.infrastructure.entity.MapLayerStyle;
import com.zynboot.map.infrastructure.entity.MapLayerVersion;
import com.zynboot.map.infrastructure.mapper.MapLayerFieldMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerStyleMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerVersionMapper;
import com.zynboot.map.response.layer.LayerRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapLayerService {

    private final LayerRepository layerRepository;
    private final SourceRepository sourceRepository;
    private final LayerQueryHandler layerQueryHandler;
    private final MapLayerFieldMapper fieldMapper;
    private final MapLayerStyleMapper styleMapper;
    private final MapLayerVersionMapper versionMapper;

    public List<LayerRes> list(String groupId) {
        return layerRepository.findByGroupId(groupId).stream()
                .map(layerQueryHandler::toRes)
                .toList();
    }

    public LayerRes getById(String id) {
        return layerQueryHandler.toRes(requireLayer(id));
    }

    @Transactional
    public void create(LayerSaveCmd cmd) {
        LayerAggregate layer = LayerAggregate.create(
                cmd.getGroupId(),
                cmd.getName(),
                cmd.getType(),
                cmd.getTargetSrid(),
                cmd.getGeometryType());
        applyLayer(layer, cmd);
        layerRepository.save(layer);
    }

    @Transactional
    public void update(String id, LayerSaveCmd cmd) {
        LayerAggregate layer = requireLayer(id);
        applyLayer(layer, cmd);
        layerRepository.update(layer);
    }

    @Transactional
    public void delete(String id) {
        requireLayer(id);
        for (SourceAggregate source : sourceRepository.findByLayerId(id)) {
            sourceRepository.delete(source.getId());
        }
        fieldMapper.delete(new LambdaQueryWrapper<MapLayerField>().eq(MapLayerField::getLayerId, id));
        styleMapper.delete(new LambdaQueryWrapper<MapLayerStyle>().eq(MapLayerStyle::getLayerId, id));
        versionMapper.delete(new LambdaQueryWrapper<MapLayerVersion>().eq(MapLayerVersion::getLayerId, id));
        layerRepository.delete(id);
    }

    private LayerAggregate requireLayer(String id) {
        return layerRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("图层"));
    }

    private void applyLayer(LayerAggregate layer, LayerSaveCmd cmd) {
        layer.updateStructure(cmd.getGroupId(), cmd.getType(), cmd.getTargetSrid(), cmd.getGeometryType());
        layer.updateInfo(cmd.getName(), cmd.getDescription(), cmd.getRenderOrder(),
                null, null, null, cmd.getMinZoom(), cmd.getMaxZoom(), cmd.getOpacity());
    }
}
