package com.zynboot.map.command.layer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "图层字段保存参数")
public class LayerFieldSaveCmd {

    private static final String FIELD_TYPE_PATTERN = "STRING|INTEGER|DOUBLE|DATE|BOOLEAN";

    @Schema(description = "字段名", example = "road_name")
    @NotBlank
    private String name;

    /** 显示别名，前端展示用 */
    @Schema(description = "字段显示别名", example = "道路名称")
    private String alias;

    /**
     * 字段类型：
     * <ul>
     *   <li>{@code STRING}   — 字符串</li>
     *   <li>{@code INTEGER}  — 整数</li>
     *   <li>{@code DOUBLE}   — 双精度浮点数</li>
     *   <li>{@code DATE}     — 日期（ISO 8601 格式，如 {@code "2024-01-15"}，存储为 JSON 字符串）</li>
     *   <li>{@code BOOLEAN}  — 布尔</li>
     * </ul>
     */
    @Schema(description = "字段类型", example = "STRING")
    @NotBlank
    @Pattern(regexp = FIELD_TYPE_PATTERN, message = "字段类型仅支持 STRING/INTEGER/DOUBLE/DATE/BOOLEAN")
    private String type;

    @Schema(description = "是否可见", example = "true")
    private Boolean visible;
    @Schema(description = "是否可排序", example = "true")
    private Boolean sortable;
    @Schema(description = "是否可搜索", example = "true")
    private Boolean searchable;
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sortOrder;
}
