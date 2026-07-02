package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.command.instance.InstanceLayerSaveCmd;
import com.zynboot.map.command.instance.InstanceSaveCmd;
import com.zynboot.map.infrastructure.entity.MapInstance;
import com.zynboot.map.infrastructure.entity.MapInstanceLayer;
import com.zynboot.map.infrastructure.entity.MapPublish;
import com.zynboot.map.infrastructure.mapper.MapInstanceLayerMapper;
import com.zynboot.map.infrastructure.mapper.MapInstanceMapper;
import com.zynboot.map.infrastructure.mapper.MapPublishMapper;
import com.zynboot.map.response.instance.InstanceLayerRes;
import com.zynboot.map.response.instance.InstanceRes;
import com.zynboot.map.response.instance.PublicMapConfigRes;
import com.zynboot.map.response.instance.PublishRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapInstanceService {

    private final MapInstanceMapper instanceMapper;
    private final MapInstanceLayerMapper instanceLayerMapper;
    private final MapPublishMapper publishMapper;

    public List<InstanceRes> listInstances() {
        return instanceMapper.selectList(null).stream().map(this::toInstanceRes).toList();
    }

    public InstanceRes getInstance(String id) {
        return toInstanceRes(requireInstance(id));
    }

    @Transactional
    public InstanceRes createInstance(InstanceSaveCmd cmd) {
        MapInstance instance = new MapInstance();
        instance.setId(IdUtils.uuid());
        applyInstance(instance, cmd);
        instanceMapper.insert(instance);
        return toInstanceRes(instanceMapper.selectById(instance.getId()));
    }

    @Transactional
    public InstanceRes updateInstance(String id, InstanceSaveCmd cmd) {
        MapInstance instance = requireInstance(id);
        applyInstance(instance, cmd);
        instanceMapper.updateById(instance);
        return toInstanceRes(instanceMapper.selectById(id));
    }

    @Transactional
    public void deleteInstance(String id) {
        requireInstance(id);
        instanceLayerMapper.delete(new LambdaQueryWrapper<MapInstanceLayer>().eq(MapInstanceLayer::getInstanceId, id));
        publishMapper.delete(new LambdaQueryWrapper<MapPublish>().eq(MapPublish::getInstanceId, id));
        instanceMapper.deleteById(id);
    }

    public List<InstanceLayerRes> getLayers(String instanceId) {
        requireInstance(instanceId);
        return instanceLayerMapper.selectList(new LambdaQueryWrapper<MapInstanceLayer>()
                        .eq(MapInstanceLayer::getInstanceId, instanceId)
                        .orderByAsc(MapInstanceLayer::getRenderOrder))
                .stream().map(this::toInstanceLayerRes).toList();
    }

    @Transactional
    public void updateLayers(String instanceId, List<InstanceLayerSaveCmd> layers) {
        requireInstance(instanceId);
        instanceLayerMapper.delete(new LambdaQueryWrapper<MapInstanceLayer>().eq(MapInstanceLayer::getInstanceId, instanceId));
        for (InstanceLayerSaveCmd cmd : layers) {
            MapInstanceLayer layer = new MapInstanceLayer();
            layer.setId(IdUtils.uuid());
            layer.setInstanceId(instanceId);
            layer.setParentId(cmd.getParentId());
            layer.setIsGroup(cmd.getIsGroup() != null ? cmd.getIsGroup() : false);
            layer.setLayerId(cmd.getLayerId());
            layer.setName(cmd.getName());
            layer.setVisible(cmd.getVisible() != null ? cmd.getVisible() : true);
            layer.setOpacity(cmd.getOpacity());
            layer.setRenderOrder(cmd.getRenderOrder() != null ? cmd.getRenderOrder() : 0);
            instanceLayerMapper.insert(layer);
        }
    }

    public List<PublishRes> listPublish(String instanceId) {
        requireInstance(instanceId);
        return publishMapper.selectList(new LambdaQueryWrapper<MapPublish>().eq(MapPublish::getInstanceId, instanceId))
                .stream().map(this::toPublishRes).toList();
    }

    @Transactional
    public PublishRes publish(String instanceId) {
        requireInstance(instanceId);
        MapPublish publish = new MapPublish();
        publish.setId(IdUtils.uuid());
        publish.setInstanceId(instanceId);
        publish.setType("PUBLIC");
        publish.setIsActive(true);
        publishMapper.insert(publish);
        return toPublishRes(publishMapper.selectById(publish.getId()));
    }

    @Transactional
    public void deletePublish(String id) {
        MapPublish publish = publishMapper.selectById(id);
        if (publish == null) throw BizException.notFound("发布记录");
        publishMapper.deleteById(id);
    }

    public InstanceRes getPublicMap(String publishId) {
        MapPublish publish = requireActivePublish(publishId);
        return toInstanceRes(requireInstance(publish.getInstanceId()));
    }

    public PublicMapConfigRes getPublicConfig(String publishId) {
        MapPublish publish = requireActivePublish(publishId);
        MapInstance instance = requireInstance(publish.getInstanceId());
        List<InstanceLayerRes> layers = instanceLayerMapper.selectList(
                        new LambdaQueryWrapper<MapInstanceLayer>()
                                .eq(MapInstanceLayer::getInstanceId, instance.getId())
                                .eq(MapInstanceLayer::getVisible, true)
                                .orderByAsc(MapInstanceLayer::getRenderOrder))
                .stream().map(this::toInstanceLayerRes).toList();
        return PublicMapConfigRes.builder()
                .instance(toInstanceRes(instance))
                .layers(layers)
                .build();
    }

    private MapInstance requireInstance(String id) {
        MapInstance instance = instanceMapper.selectById(id);
        if (instance == null) throw BizException.notFound("地图实例");
        return instance;
    }

    private MapPublish requireActivePublish(String publishId) {
        MapPublish publish = publishMapper.selectById(publishId);
        if (publish == null || !Boolean.TRUE.equals(publish.getIsActive())) {
            throw BizException.notFound("发布记录");
        }
        return publish;
    }

    private void applyInstance(MapInstance instance, InstanceSaveCmd cmd) {
        instance.setName(cmd.getName());
        instance.setDescription(cmd.getDescription());
        instance.setCenterLng(cmd.getCenterLng());
        instance.setCenterLat(cmd.getCenterLat());
        instance.setZoom(cmd.getZoom() != null ? cmd.getZoom() : 10);
        instance.setBasemapId(cmd.getBasemapId());
        instance.setIsPublic(cmd.getIsPublic() != null ? cmd.getIsPublic() : false);
    }

    private InstanceRes toInstanceRes(MapInstance instance) {
        return InstanceRes.builder()
                .id(instance.getId())
                .name(instance.getName())
                .description(instance.getDescription())
                .centerLng(instance.getCenterLng())
                .centerLat(instance.getCenterLat())
                .zoom(instance.getZoom())
                .basemapId(instance.getBasemapId())
                .isPublic(instance.getIsPublic())
                .createTime(instance.getCreateTime())
                .updateTime(instance.getUpdateTime())
                .build();
    }

    private InstanceLayerRes toInstanceLayerRes(MapInstanceLayer layer) {
        return InstanceLayerRes.builder()
                .id(layer.getId())
                .instanceId(layer.getInstanceId())
                .parentId(layer.getParentId())
                .isGroup(layer.getIsGroup())
                .layerId(layer.getLayerId())
                .name(layer.getName())
                .visible(layer.getVisible())
                .opacity(layer.getOpacity())
                .renderOrder(layer.getRenderOrder())
                .build();
    }

    private PublishRes toPublishRes(MapPublish publish) {
        return PublishRes.builder()
                .id(publish.getId())
                .instanceId(publish.getInstanceId())
                .type(publish.getType())
                .isActive(publish.getIsActive())
                .createTime(publish.getCreateTime())
                .updateTime(publish.getUpdateTime())
                .build();
    }
}
