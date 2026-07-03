package com.zynboot.map.command.group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图层分组保存参数")
public class GroupSaveCmd {

    @Schema(description = "分组 ID，更新时可传", example = "f4f7c0b84efb4c4b9e6f433d49bff1bd")
    String id;
    @Schema(description = "父级分组 ID，根分组可为空", example = "root")
    String parentId;

    @Schema(description = "分组名称", example = "基础地理")
    @NotBlank(message = "分组名称不能为空")
    String name;

    @Schema(description = "分组描述", example = "存放行政区、道路、水系等基础图层")
    String description;
    @Schema(description = "排序值，越小越靠前", example = "1")
    Integer sortOrder;
    @Schema(description = "图标标识", example = "folder")
    String icon;
    @Schema(description = "分组颜色", example = "#1677ff")
    String color;
}
