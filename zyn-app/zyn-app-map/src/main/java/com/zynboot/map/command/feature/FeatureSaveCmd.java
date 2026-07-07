package com.zynboot.map.command.feature;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "要素保存参数")
public class FeatureSaveCmd {

    @Schema(description = "要素属性", example = "{\"name\":\"示例点位\",\"level\":1}")
    @NotNull
    private JsonNode properties;

    /**
     * GeoJSON geometry.
     */
    @Schema(description = "GeoJSON geometry", example = "{\"type\":\"Point\",\"coordinates\":[113.2644,23.1291]}")
    @NotNull
    private JsonNode geometry;
}
