package com.zynboot.infra.storage.model;

public record StoredObjectMetadata(
        String key,
        String contentType,
        long size,
        String accessUrl
) {
}
