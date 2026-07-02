package com.zynboot.map.command.instance;

import lombok.Data;

@Data
public class InstanceLayerSaveCmd {
    private String parentId;
    private Boolean isGroup;
    private String layerId;
    private String name;
    private Boolean visible;
    private Double opacity;
    private Integer renderOrder;
}
