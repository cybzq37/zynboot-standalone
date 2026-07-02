package com.zynboot.infra.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@ConfigurationProperties(prefix = "zyn.storage")
public class StorageProperties {

    private StorageType type = StorageType.LOCAL;
    private String publicBaseUrl;
    private String datePathPattern = "yyyy/MM/dd";
    private StorageFilenameStrategy filenameStrategy = StorageFilenameStrategy.UUID;
    private StorageConflictStrategy conflictStrategy = StorageConflictStrategy.APPEND_SUFFIX;
    private long maxInMemoryReadBytes = 10 * 1024 * 1024L;
    private LocalProperties local = new LocalProperties();
    private S3Properties s3 = new S3Properties();

    public void validate() {
        if (!StringUtils.hasText(datePathPattern)) {
            throw new IllegalStateException("zyn.storage.date-path-pattern must not be blank");
        }
        try {
            DateTimeFormatter.ofPattern(datePathPattern.trim());
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new IllegalStateException("Invalid zyn.storage.date-path-pattern: " + datePathPattern, exception);
        }
        if (maxInMemoryReadBytes <= 0) {
            throw new IllegalStateException("zyn.storage.max-in-memory-read-bytes must be greater than 0");
        }

        if (type == StorageType.LOCAL) {
            validateLocal();
            return;
        }
        validateS3();
    }

    private void validateLocal() {
        if (local == null || !StringUtils.hasText(local.getRootPath())) {
            throw new IllegalStateException("zyn.storage.local.root-path must not be blank");
        }
    }

    private void validateS3() {
        if (s3 == null || !StringUtils.hasText(s3.getBucket())) {
            throw new IllegalStateException("zyn.storage.s3.bucket must not be blank");
        }
        boolean hasAccessKey = StringUtils.hasText(s3.getAccessKey());
        boolean hasSecretKey = StringUtils.hasText(s3.getSecretKey());
        if (hasAccessKey != hasSecretKey) {
            throw new IllegalStateException("Both zyn.storage.s3.access-key and secret-key must be configured together");
        }
    }

    @Data
    public static class LocalProperties {
        private String rootPath = "./uploads";
        private String accessPathPrefix = "/uploads";
    }

    @Data
    public static class S3Properties {
        private String bucket = "zyn";
        private String endpoint;
        private String region = "cn-north-1";
        private String accessKey;
        private String secretKey;
        private String domain;
        private boolean pathStyleAccess = true;
    }
}
