package com.zynboot.map.command.feature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zynboot.kit.jackson.deserializer.LenientJsonNodeDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 要素合并参数：删除多个源要素，创建一个新要素（原子事务）。
 */
@Data
@Schema(description = "要素合并参数：删除多个源要素，创建一个新要素")
public class FeatureMergeCmd {

    @Schema(description = "被合并的源要素 ID 列表（至少 2 个）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Size(min = 2, message = "合并的源要素至少 2 个")
    private List<Long> originIds;

    @Schema(description = "合并后的目标要素", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Valid
    private FeatureMergeTarget target;

    /**
     * 合并后的目标要素。
     * geometry 可不传，由服务端用 ST_Union 计算源要素几何并集。
     */
    @Data
    @Schema(description = "合并后的目标要素")
    public static class FeatureMergeTarget {

        @Schema(description = "要素属性，支持 JSON 对象或 JSON 字符串", example = "{\"name\":\"合并要素\"}")
        @NotNull
        @JsonDeserialize(using = LenientJsonNodeDeserializer.class)
        private JsonNode properties;

        @Schema(description = "GeoJSON geometry，不传时由服务端用 ST_Union 计算源要素几何并集")
        @JsonDeserialize(using = LenientJsonNodeDeserializer.class)
        private JsonNode geometry;
    }
}
