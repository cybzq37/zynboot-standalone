package com.zynboot.map.command.layer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LayerFieldSaveCmd {

    private static final String FIELD_TYPE_PATTERN = "STRING|INTEGER|DOUBLE|DATE|BOOLEAN";

    @NotBlank
    private String name;

    /** 显示别名，前端展示用 */
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
    @NotBlank
    @Pattern(regexp = FIELD_TYPE_PATTERN, message = "字段类型仅支持 STRING/INTEGER/DOUBLE/DATE/BOOLEAN")
    private String type;

    private Boolean visible;
    private Boolean sortable;
    private Boolean searchable;
    private Integer sortOrder;
}
