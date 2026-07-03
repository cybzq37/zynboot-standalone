package com.zynboot.map.command.instance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "地图实例图层树节点保存参数")
public class InstanceLayerSaveCmd {
    @Schema(description = "父节点 ID，根节点可为空", example = "group-root")
    private String parentId;
    @Schema(description = "当前节点是否为分组", example = "false")
    private Boolean isGroup;
    @Schema(description = "图层 ID，isGroup=false 时使用", example = "3d74d7a5f9b04b43a9b07b0fdc30e6bf")
    private String layerId;
    @Schema(description = "节点显示名称", example = "道路图层")
    private String name;
    @Schema(description = "是否默认可见", example = "true")
    private Boolean visible;
    @Schema(description = "透明度", example = "0.8")
    private Double opacity;
    @Schema(description = "渲染顺序，越大越靠上", example = "100")
    private Integer renderOrder;
}
