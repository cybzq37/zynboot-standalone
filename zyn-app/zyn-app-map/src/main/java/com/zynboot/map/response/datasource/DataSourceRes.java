package com.zynboot.map.response.datasource;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class DataSourceRes {
    String id;
    String name;
    String type;
    String url;
    String username;
    String schemaName;
    String driverClass;
    String testQuery;
    String status;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
