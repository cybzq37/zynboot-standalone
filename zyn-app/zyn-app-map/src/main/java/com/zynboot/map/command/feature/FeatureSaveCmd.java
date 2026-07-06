package com.zynboot.map.command.feature;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "要素保存参数")
public class FeatureSaveCmd {

    @Schema(description = "来源数据源 ID；手动新增要素时不传，存为 NULL", example = "6f7d71de0c9148e4ac3a793ee39d19cb")
    private String sourceId;

    @Schema(description = "要素属性 JSON 字符串", example = "{\"name\":\"示例点位\",\"level\":1}")
    @NotBlank
    private String properties;

    /**
     * GeoJSON geometry.
     */
    @Schema(description = "GeoJSON geometry 字符串", example = "{\"type\":\"Point\",\"coordinates\":[113.2644,23.1291]}")
    @NotBlank
    private String geometry;
}
