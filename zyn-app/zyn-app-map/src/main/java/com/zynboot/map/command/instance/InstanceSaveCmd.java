package com.zynboot.map.command.instance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "地图实例保存参数")
public class InstanceSaveCmd {
    @Schema(description = "实例名称", example = "城市治理一张图")
    @NotBlank
    private String name;
    @Schema(description = "实例描述", example = "供内部业务系统使用的综合地图实例")
    private String description;
    @Schema(description = "中心点经度", example = "113.2644")
    private Double centerLng;
    @Schema(description = "中心点纬度", example = "23.1291")
    private Double centerLat;
    @Schema(description = "默认缩放级别", example = "12")
    private Integer zoom;
    @Schema(description = "默认底图 ID", example = "7da7cfe6cf9b4b52af8f3d7e18f2af4b")
    private String basemapId;
    @Schema(description = "是否允许公开访问", example = "false")
    private Boolean isPublic;
}
