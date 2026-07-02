package com.zynboot.map.response.audit;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AuditLogRes {
    String id;
    String targetType;
    String targetId;
    String action;
    String operatorId;
    String operatorName;
    String detailJson;
    String ip;
    LocalDateTime createTime;
}
