package com.zynboot.infra.storage.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BatchDeleteResult {

    int requestedCount;
    int successCount;
    int failureCount;

    @Singular("item")
    List<BatchDeleteItemResult> items;
}
