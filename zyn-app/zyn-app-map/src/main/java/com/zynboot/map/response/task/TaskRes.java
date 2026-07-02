package com.zynboot.map.response.task;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskRes {
    String id;
    String type;
    String sourceId;
    String layerId;
    String status;
    Integer progress;
    Integer totalCount;
    Integer processedCount;
    Integer errorCount;
    String errorMessage;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    LocalDateTime createdAt;
}
