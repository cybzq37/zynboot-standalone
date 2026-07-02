package com.zynboot.map.command.layer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LayerSaveCmd {

    String id;
    String groupId;

    @NotBlank(message = "图层名称不能为空")
    String name;

    String title;
    String description;

    @NotBlank(message = "图层类型不能为空")
    String type;

    @NotNull(message = "目标坐标系不能为空")
    Integer targetSrid;

    String geometryType;
    Integer renderOrder;
    Integer minZoom;
    Integer maxZoom;
    Double opacity;
}
