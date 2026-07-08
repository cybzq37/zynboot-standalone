package com.zynboot.map.command.feature;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 要素拆分参数：删除一个源要素，创建多个新要素（原子事务）。
 */
@Data
@Schema(description = "要素拆分参数：删除源要素，创建多个新要素")
public class FeatureSplitCmd {

    @Schema(description = "被拆分的源要素 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long originId;

    @Schema(description = "拆分后的新要素数组（至少 2 个）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Size(min = 2, message = "拆分后的新要素至少 2 个")
    @Valid
    private List<FeatureSaveCmd> targets;
}
