package com.zynboot.map.domain.aggregate;

import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.infrastructure.entity.MapLayerSource;

public class SourceAggregate {

    private final MapLayerSource entity;

    private SourceAggregate(MapLayerSource entity) {
        this.entity = entity;
    }

    public static SourceAggregate from(MapLayerSource entity) {
        return new SourceAggregate(entity);
    }

    public static SourceAggregate create(String layerId, String name, String type, String format) {
        MapLayerSource source = new MapLayerSource();
        source.setId(IdUtils.uuid());
        source.setLayerId(layerId);
        source.setName(name);
        source.setType(type);
        source.setFormat(format);
        source.setFeatureCount(0);
        source.setStatus("PENDING");
        return new SourceAggregate(source);
    }

    public MapLayerSource getEntity() { return entity; }

    public String getId() { return entity.getId(); }
    public String getLayerId() { return entity.getLayerId(); }
    public String getType() { return entity.getType(); }
    public String getFormat() { return entity.getFormat(); }
    public String getStatus() { return entity.getStatus(); }
    public Integer getFeatureCount() { return entity.getFeatureCount(); }

    public void markCompleted(int featureCount) {
        entity.setFeatureCount(featureCount);
        entity.setStatus("COMPLETED");
    }

    public void markFailed(String message) {
        entity.setStatus("FAILED");
        entity.setMessage(message);
    }
}
