package com.zynboot.map.response.layer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Value
@Jacksonized
@Builder
@AllArgsConstructor
public class LayerRes {

    String id;
    String groupId;
    String name;
    String title;
    String description;
    String type;
    Integer targetSrid;
    String geometryType;
    Integer featureCount;
    Integer sourceCount;
    Integer renderOrder;
    Boolean visible;
    Boolean selectable;
    Boolean editable;
    Boolean isPublic;
    Integer minZoom;
    Integer maxZoom;
    Double opacity;
    LocalDateTime createTime;
}
