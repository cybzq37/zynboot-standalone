package com.zynboot.map.response.instance;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PublishRes {
    String id;
    String instanceId;
    String type;
    Boolean isActive;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
