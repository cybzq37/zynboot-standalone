package com.zynboot.map.command.datasource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataSourceSaveCmd {
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    @NotBlank
    private String url;
    private String username;
    private String password;
    private String schemaName;
    private String driverClass;
    private String testQuery;
    private String status;
}
