package com.zynboot.map.domain.aggregate;

import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.infrastructure.entity.MapLayerGroup;

public class LayerGroupAggregate {

    private final MapLayerGroup entity;

    private LayerGroupAggregate(MapLayerGroup entity) {
        this.entity = entity;
    }

    public static LayerGroupAggregate from(MapLayerGroup entity) {
        return new LayerGroupAggregate(entity);
    }

    public static LayerGroupAggregate create(String parentId, String name) {
        MapLayerGroup group = new MapLayerGroup();
        group.setId(IdUtils.uuid());
        group.setParentId(parentId);
        group.setName(name);
        group.setSortOrder(0);
        return new LayerGroupAggregate(group);
    }

    public MapLayerGroup getEntity() { return entity; }

    public String getId() { return entity.getId(); }
    public String getName() { return entity.getName(); }

    public void updateInfo(String name, String description, Integer sortOrder, String icon, String color) {
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (sortOrder != null) entity.setSortOrder(sortOrder);
        if (icon != null) entity.setIcon(icon);
        if (color != null) entity.setColor(color);
    }
}
