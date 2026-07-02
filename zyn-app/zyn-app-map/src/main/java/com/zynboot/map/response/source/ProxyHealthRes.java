package com.zynboot.map.response.source;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ProxyHealthRes {
    String sourceId;
    String url;
    String authType;
    Integer cacheTtl;
    String healthStatus;
    String healthMessage;
    LocalDateTime lastCheckAt;
    Integer failCount;
}
