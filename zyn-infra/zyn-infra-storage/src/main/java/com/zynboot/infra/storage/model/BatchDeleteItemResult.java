package com.zynboot.infra.storage.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BatchDeleteItemResult {

    String requestedKey;
    String resolvedKey;
    boolean success;
    String message;
}
