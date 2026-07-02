package com.zynboot.map.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("map_async_task")
public class MapAsyncTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    private String type;
    private String sourceId;
    private String layerId;
    private String status;
    private Integer progress;
    private Integer totalCount;
    private Integer processedCount;
    private Integer errorCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
