package com.zynboot.map.command.layer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "图层样式保存参数")
public class LayerStyleSaveCmd {
    @Schema(description = "样式名称", example = "道路默认样式")
    @NotBlank
    private String name;
    @Schema(description = "样式类型", example = "VECTOR")
    @NotBlank
    private String type;
    @Schema(description = "样式 JSON 配置", example = "{\"lineColor\":\"#1677ff\",\"lineWidth\":2}")
    @NotBlank
    private String styleJson;
    @Schema(description = "是否设为默认样式", example = "true")
    private Boolean isDefault;
}
