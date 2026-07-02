package com.zynboot.infra.storage.support;

import com.zynboot.infra.storage.config.StorageFilenameStrategy;
import com.zynboot.infra.storage.exception.StorageInvalidKeyException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class StorageObjectKeyGenerator {

    private final DateTimeFormatter datePathFormatter;
    private final StorageFilenameStrategy filenameStrategy;

    public StorageObjectKeyGenerator(String datePathPattern, StorageFilenameStrategy filenameStrategy) {
        String pattern = StringUtils.hasText(datePathPattern) ? datePathPattern.trim() : "yyyy/MM/dd";
        try {
            this.datePathFormatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid storage date path pattern: " + datePathPattern, exception);
        }
        this.filenameStrategy = Objects.requireNonNullElse(filenameStrategy, StorageFilenameStrategy.UUID);
    }

    public String generate(String originalFilename) {
        String datePath = LocalDate.now().format(datePathFormatter);
        return normalizeKey(datePath + "/" + buildStoredFilename(originalFilename));
    }

    public String generateUnderPrefix(String prefix, String originalFilename) {
        String datePath = LocalDate.now().format(datePathFormatter);
        return joinPath(prefix, datePath, buildStoredFilename(originalFilename));
    }

    public String buildStoredFilename(String originalFilename) {
        String extension = extractExtension(originalFilename);
        String baseName = filenameStrategy == StorageFilenameStrategy.ORIGINAL
                ? sanitizeBaseName(originalFilename)
                : UUID.randomUUID().toString().replace("-", "");
        return extension.isEmpty() ? baseName : baseName + "." + extension;
    }

    public static String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return "";
        }
        String normalized = key.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }

    public static String requireValidKey(String key, String fieldName) {
        String normalized = normalizeKey(key);
        if (!StringUtils.hasText(normalized)) {
            throw new StorageInvalidKeyException(fieldName + " must not be blank");
        }
        if (normalized.endsWith("/")) {
            throw new StorageInvalidKeyException(fieldName + " must not end with '/'");
        }
        validateSegments(normalized, fieldName);
        return normalized;
    }

    public static String requireValidPrefix(String prefix, String fieldName) {
        String normalized = normalizeKey(prefix);
        if (!StringUtils.hasText(normalized)) {
            throw new StorageInvalidKeyException(fieldName + " must not be blank");
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new StorageInvalidKeyException(fieldName + " must not be blank");
        }
        validateSegments(normalized, fieldName);
        return normalized;
    }

    public static String extractFilename(String key) {
        if (!StringUtils.hasText(key)) {
            return "";
        }
        String normalized = normalizeKey(key);
        int lastSlashIndex = normalized.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalized.substring(lastSlashIndex + 1) : normalized;
    }

    public static String appendNumericSuffix(String key, int suffix) {
        String normalizedKey = normalizeKey(key);
        if (suffix <= 0 || !StringUtils.hasText(normalizedKey)) {
            return normalizedKey;
        }
        int lastSlashIndex = normalizedKey.lastIndexOf('/');
        String directory = lastSlashIndex >= 0 ? normalizedKey.substring(0, lastSlashIndex) : "";
        String filename = lastSlashIndex >= 0 ? normalizedKey.substring(lastSlashIndex + 1) : normalizedKey;
        int lastDotIndex = filename.lastIndexOf('.');
        String baseName = lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
        String extension = lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
        String suffixedFilename = baseName + "_" + suffix + extension;
        return directory.isEmpty() ? suffixedFilename : directory + "/" + suffixedFilename;
    }

    public static String joinPath(String... segments) {
        StringBuilder builder = new StringBuilder();
        if (segments == null) {
            return "";
        }
        for (String segment : segments) {
            String normalized = normalizeKey(segment);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('/');
            }
            builder.append(normalized);
        }
        return builder.toString();
    }

    public static String encodeUrlPath(String key) {
        String normalizedKey = normalizeKey(key);
        if (!StringUtils.hasText(normalizedKey)) {
            return "";
        }
        String[] segments = normalizedKey.split("/");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (!builder.isEmpty()) {
                builder.append('/');
            }
            builder.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return builder.toString();
    }

    private String extractExtension(String originalFilename) {
        String filename = extractLeafFilename(originalFilename);
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeBaseName(String originalFilename) {
        String filename = extractLeafFilename(originalFilename);
        String baseName = StringUtils.hasText(filename)
                ? StringUtils.stripFilenameExtension(filename)
                : "file";
        StringBuilder builder = new StringBuilder(baseName.length());
        for (char current : baseName.toCharArray()) {
            if (Character.isLetterOrDigit(current) || current == '-' || current == '_') {
                builder.append(current);
            } else {
                builder.append('_');
            }
        }
        String sanitized = builder.toString()
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        return StringUtils.hasText(sanitized) ? sanitized : "file";
    }

    private String extractLeafFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return null;
        }
        return StringUtils.getFilename(originalFilename.replace('\\', '/'));
    }

    private static void validateSegments(String normalized, String fieldName) {
        int start = 0;
        while (start <= normalized.length()) {
            int end = normalized.indexOf('/', start);
            if (end < 0) {
                end = normalized.length();
            }
            String segment = normalized.substring(start, end);
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new StorageInvalidKeyException(fieldName + " contains invalid path segment: " + normalized);
            }
            start = end + 1;
        }
    }

    public static String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
