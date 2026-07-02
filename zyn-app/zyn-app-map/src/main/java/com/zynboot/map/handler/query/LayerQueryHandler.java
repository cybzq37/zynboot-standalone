package com.zynboot.map.handler.query;

import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.infrastructure.entity.MapLayer;
import com.zynboot.map.response.layer.LayerRes;
import org.springframework.stereotype.Component;

@Component
public class LayerQueryHandler {

    public LayerRes toRes(LayerAggregate agg) {
        MapLayer e = agg.getEntity();
        return LayerRes.builder()
                .id(e.getId())
                .groupId(e.getGroupId())
                .name(e.getName())
                .title(e.getTitle())
                .description(e.getDescription())
                .type(e.getType())
                .targetSrid(e.getTargetSrid())
                .geometryType(e.getGeometryType())
                .featureCount(e.getFeatureCount())
                .sourceCount(e.getSourceCount())
                .renderOrder(e.getRenderOrder())
                .visible(e.getVisible())
                .selectable(e.getSelectable())
                .editable(e.getEditable())
                .isPublic(e.getIsPublic())
                .minZoom(e.getMinZoom())
                .maxZoom(e.getMaxZoom())
                .opacity(e.getOpacity())
                .createTime(e.getCreateTime())
                .build();
    }
}
