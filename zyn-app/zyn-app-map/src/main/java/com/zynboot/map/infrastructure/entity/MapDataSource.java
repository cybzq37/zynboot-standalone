package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_data_source")
public class MapDataSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String name;
    private String type;
    private String url;
    private String username;
    private String password;
    private String schemaName;
    private String driverClass;
    private String testQuery;
    private String status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
