package com.zynboot.map.command.datasource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "外部数据源保存参数")
public class DataSourceSaveCmd {
    @Schema(description = "数据源名称", example = "生产 PostGIS")
    @NotBlank
    private String name;
    @Schema(description = "数据源类型", example = "POSTGIS")
    @NotBlank
    private String type;
    @Schema(description = "连接地址", example = "jdbc:postgresql://localhost:5432/gis")
    @NotBlank
    private String url;
    @Schema(description = "用户名", example = "postgres")
    private String username;
    @Schema(description = "密码", example = "secret")
    private String password;
    @Schema(description = "默认 schema", example = "public")
    private String schemaName;
    @Schema(description = "驱动类名", example = "org.postgresql.Driver")
    private String driverClass;
    @Schema(description = "连通性测试 SQL", example = "select 1")
    private String testQuery;
    @Schema(description = "状态", example = "ENABLED")
    private String status;
}
