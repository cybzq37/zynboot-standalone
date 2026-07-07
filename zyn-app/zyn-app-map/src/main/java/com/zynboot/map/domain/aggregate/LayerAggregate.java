package com.zynboot.map.domain.aggregate;

import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.infrastructure.entity.MapLayer;

public class LayerAggregate {

    private final MapLayer entity;

    private LayerAggregate(MapLayer entity) {
        this.entity = entity;
    }

    public static LayerAggregate from(MapLayer entity) {
        return new LayerAggregate(entity);
    }

    public static LayerAggregate create(String groupId, String name, String type, Integer targetSrid, String geometryType) {
        MapLayer layer = new MapLayer();
        layer.setId(IdUtils.uuid());
        layer.setGroupId(groupId);
        layer.setName(name);
        layer.setType(type);
        layer.setTargetSrid(targetSrid);
        layer.setGeometryType(geometryType);
        layer.setFeatureCount(0);
        layer.setSourceCount(0);
        layer.setRenderOrder(0);
        layer.setVisible(true);
        layer.setSelectable(true);
        layer.setEditable(true);
        layer.setIsPublic(false);
        layer.setMinZoom(0);
        layer.setMaxZoom(24);
        layer.setOpacity(1.0);
        return new LayerAggregate(layer);
    }

    public MapLayer getEntity() { return entity; }

    public String getId() { return entity.getId(); }
    public String getName() { return entity.getName(); }
    public String getType() { return entity.getType(); }
    public Integer getTargetSrid() { return entity.getTargetSrid(); }
    public Integer getFeatureCount() { return entity.getFeatureCount(); }
    public Integer getSourceCount() { return entity.getSourceCount(); }

    public void updateInfo(String name, String description, Integer renderOrder,
                           Boolean visible, Boolean selectable, Boolean editable,
                           Integer minZoom, Integer maxZoom, Double opacity) {
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (renderOrder != null) entity.setRenderOrder(renderOrder);
        if (visible != null) entity.setVisible(visible);
        if (selectable != null) entity.setSelectable(selectable);
        if (editable != null) entity.setEditable(editable);
        if (minZoom != null) entity.setMinZoom(minZoom);
        if (maxZoom != null) entity.setMaxZoom(maxZoom);
        if (opacity != null) entity.setOpacity(opacity);
    }

    public void updateStructure(String groupId, String type, Integer targetSrid, String geometryType) {
        if (groupId != null) entity.setGroupId(groupId);
        if (type != null) entity.setType(type);
        if (targetSrid != null) entity.setTargetSrid(targetSrid);
        if (geometryType != null) entity.setGeometryType(geometryType);
    }

    public void incrementFeatureCount(int count) {
        entity.setFeatureCount(entity.getFeatureCount() + count);
    }

    public void incrementSourceCount() {
        entity.setSourceCount(entity.getSourceCount() + 1);
    }
}
