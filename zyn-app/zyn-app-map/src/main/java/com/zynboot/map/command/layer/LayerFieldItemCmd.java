package com.zynboot.map.command.layer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "图层字段项（批量保存用）")
public class LayerFieldItemCmd {

    private static final String FIELD_TYPE_PATTERN = "STRING|INTEGER|DOUBLE|DATE|BOOLEAN";

    @Schema(description = "字段 ID；为空表示新建，非空表示更新", example = "abc123")
    private String id;

    @Schema(description = "字段名", example = "road_name")
    @NotBlank
    private String name;

    @Schema(description = "字段显示别名", example = "道路名称")
    private String alias;

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
    @Schema(description = "是否必填（新增/修改要素时校验）", example = "false")
    private Boolean required;
    @Schema(description = "默认值（字符串形式，按字段类型解析；为空时按类型自动填充零值）", example = "0")
    private String defaultValue;
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sortOrder;
}
