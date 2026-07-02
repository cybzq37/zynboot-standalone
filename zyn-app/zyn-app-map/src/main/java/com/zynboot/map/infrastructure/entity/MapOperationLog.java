package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_operation_log")
public class MapOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String targetType;
    private String targetId;
    private String action;
    private String operatorId;
    private String operatorName;
    private String detailJson;
    private String ip;
    private LocalDateTime createTime;
}
