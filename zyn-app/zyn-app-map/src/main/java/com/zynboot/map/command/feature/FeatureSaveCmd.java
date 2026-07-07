package com.zynboot.map.command.feature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zynboot.kit.jackson.deserializer.LenientJsonNodeDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "要素保存参数")
public class FeatureSaveCmd {

    @Schema(description = "要素属性，支持 JSON 对象或 JSON 字符串", example = "{\"name\":\"示例点位\",\"level\":1}")
    @NotNull
    @JsonDeserialize(using = LenientJsonNodeDeserializer.class)
    private JsonNode properties;

    /**
     * GeoJSON geometry.
     */
    @Schema(description = "GeoJSON geometry，支持 JSON 对象或 JSON 字符串", example = "{\"type\":\"Point\",\"coordinates\":[113.2644,23.1291]}")
    @NotNull
    @JsonDeserialize(using = LenientJsonNodeDeserializer.class)
    private JsonNode geometry;
}
