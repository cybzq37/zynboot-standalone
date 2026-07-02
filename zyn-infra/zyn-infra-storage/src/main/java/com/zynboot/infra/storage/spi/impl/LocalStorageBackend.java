package com.zynboot.infra.storage.spi.impl;

import com.zynboot.infra.storage.config.StorageConflictStrategy;
import com.zynboot.infra.storage.model.BatchDeleteResult;
import com.zynboot.infra.storage.model.BatchDeleteItemResult;
import com.zynboot.infra.storage.config.StorageProperties;
import com.zynboot.infra.storage.config.StorageType;
import com.zynboot.infra.storage.exception.StorageBackendException;
import com.zynboot.infra.storage.exception.StorageConflictException;
import com.zynboot.infra.storage.exception.StorageObjectNotFoundException;
import com.zynboot.infra.storage.model.FileUploadRequest;
import com.zynboot.infra.storage.model.StoredObject;
import com.zynboot.infra.storage.model.StoredObjectMetadata;
import com.zynboot.infra.storage.model.UploadedFileInfo;
import com.zynboot.infra.storage.spi.StorageBackend;
import com.zynboot.infra.storage.support.StorageObjectKeyGenerator;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public class LocalStorageBackend implements StorageBackend {

    private static final int MAX_SUFFIX_RETRIES = 1000;

    private final StorageProperties properties;
    private final Path rootPath;

    public LocalStorageBackend(StorageProperties properties) throws IOException {
        this.properties = properties;
        String configuredRootPath = properties.getLocal().getRootPath();
        if (!StringUtils.hasText(configuredRootPath)) {
            throw new IllegalStateException("zyn.infra.storage.local.root-path must not be blank");
        }
        this.rootPath = Path.of(configuredRootPath).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
    }

    @Override
    public UploadedFileInfo upload(String key,
                                   FileUploadRequest request,
                                   StorageConflictStrategy conflictStrategy) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        return switch (resolveConflictStrategy(conflictStrategy)) {
            case OVERWRITE -> writeOverwrite(normalizedKey, request);
            case FAIL -> writeFailIfExists(normalizedKey, request);
            case APPEND_SUFFIX -> writeAppendSuffix(normalizedKey, request);
        };
    }

    @Override
    public StoredObject open(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        Path targetPath = resolvePath(normalizedKey);
        StoredObjectMetadata metadata = getMetadata(normalizedKey);
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(targetPath);
        } catch (IOException exception) {
            throw new StorageBackendException("Failed to open object from local storage: " + normalizedKey, exception);
        }
        return new StoredObject(normalizedKey, metadata.contentType(), metadata.size(), inputStream);
    }

    @Override
    public StoredObjectMetadata getMetadata(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        Path targetPath = resolvePath(normalizedKey);
        requireRegularFile(targetPath, normalizedKey);
        String contentType = Files.probeContentType(targetPath);
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        long size = Files.size(targetPath);
        return new StoredObjectMetadata(normalizedKey, contentType, size, buildAccessUrl(normalizedKey));
    }

    @Override
    public boolean exists(String key) throws IOException {
        Path targetPath = resolvePath(StorageObjectKeyGenerator.requireValidKey(key, "key"));
        return Files.isRegularFile(targetPath);
    }

    @Override
    public void copy(String sourceKey, String targetKey) throws IOException {
        String normalizedSourceKey = StorageObjectKeyGenerator.requireValidKey(sourceKey, "sourceKey");
        String normalizedTargetKey = StorageObjectKeyGenerator.requireValidKey(targetKey, "targetKey");
        Path sourcePath = resolvePath(normalizedSourceKey);
        Path targetPath = resolvePath(normalizedTargetKey);
        requireRegularFile(sourcePath, normalizedSourceKey);
        rejectNonRegularExistingTarget(targetPath, normalizedTargetKey);
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void move(String sourceKey, String targetKey) throws IOException {
        String normalizedSourceKey = StorageObjectKeyGenerator.requireValidKey(sourceKey, "sourceKey");
        String normalizedTargetKey = StorageObjectKeyGenerator.requireValidKey(targetKey, "targetKey");
        Path sourcePath = resolvePath(normalizedSourceKey);
        Path targetPath = resolvePath(normalizedTargetKey);
        requireRegularFile(sourcePath, normalizedSourceKey);
        rejectNonRegularExistingTarget(targetPath, normalizedTargetKey);
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public String getAccessUrl(String key) {
        return buildAccessUrl(StorageObjectKeyGenerator.requireValidKey(key, "key"));
    }

    @Override
    public BatchDeleteResult deleteBatch(Collection<String> keys) {
        BatchDeleteResult.BatchDeleteResultBuilder builder = BatchDeleteResult.builder()
                .requestedCount(keys.size());
        int successCount = 0;
        int failureCount = 0;
        for (String key : keys) {
            String normalizedKey = StorageObjectKeyGenerator.normalizeKey(key);
            try {
                delete(normalizedKey);
                builder.item(BatchDeleteItemResult.builder()
                        .requestedKey(key)
                        .resolvedKey(normalizedKey)
                        .success(true)
                        .message(null)
                        .build());
                successCount++;
            } catch (IOException exception) {
                builder.item(BatchDeleteItemResult.builder()
                        .requestedKey(key)
                        .resolvedKey(normalizedKey)
                        .success(false)
                        .message(exception.getMessage())
                        .build());
                failureCount++;
            }
        }
        builder.successCount(successCount);
        builder.failureCount(failureCount);
        return builder.build();
    }

    @Override
    public void delete(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        Path targetPath = resolvePath(normalizedKey);
        if (Files.notExists(targetPath)) {
            throw new StorageObjectNotFoundException("Storage object not found: " + normalizedKey);
        }
        requireRegularFile(targetPath, normalizedKey);
        Files.deleteIfExists(targetPath);
        cleanupEmptyParentDirectories(targetPath.getParent());
    }

    private String buildAccessUrl(String key) {
        String encodedKey = StorageObjectKeyGenerator.encodeUrlPath(key);
        String publicBaseUrl = StorageObjectKeyGenerator.stripTrailingSlash(properties.getPublicBaseUrl());
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl + "/" + encodedKey;
        }

        String accessPathPrefix = properties.getLocal().getAccessPathPrefix();
        String normalizedPrefix = StringUtils.hasText(accessPathPrefix) ? accessPathPrefix.trim() : "/uploads";
        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        normalizedPrefix = StorageObjectKeyGenerator.stripTrailingSlash(normalizedPrefix);
        return normalizedPrefix + "/" + encodedKey;
    }

    private Path resolvePath(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.normalizeKey(key);
        Path targetPath = rootPath.resolve(normalizedKey).normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new IOException("Invalid storage key: " + key);
        }
        return targetPath;
    }

    private void requireRegularFile(Path targetPath, String key) throws IOException {
        if (!Files.isRegularFile(targetPath)) {
            throw new StorageObjectNotFoundException("Storage object not found: " + key);
        }
    }

    private void rejectNonRegularExistingTarget(Path targetPath, String key) throws IOException {
        if (Files.exists(targetPath) && !Files.isRegularFile(targetPath)) {
            throw new StorageConflictException("Storage target is not a regular file: " + key);
        }
    }

    private UploadedFileInfo writeOverwrite(String normalizedKey, FileUploadRequest request) throws IOException {
        Path targetPath = resolvePath(normalizedKey);
        rejectNonRegularExistingTarget(targetPath, normalizedKey);
        createParentDirectories(targetPath);
        try (InputStream inputStream = request.openStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            validateWrittenSize(targetPath, request.getSize(), normalizedKey);
            return buildUploadedFileInfo(normalizedKey, targetPath, request);
        } catch (IOException exception) {
            throw new StorageBackendException("Failed to upload object to local storage: " + normalizedKey, exception);
        }
    }

    private UploadedFileInfo writeFailIfExists(String normalizedKey, FileUploadRequest request) throws IOException {
        Path targetPath = resolvePath(normalizedKey);
        rejectNonRegularExistingTarget(targetPath, normalizedKey);
        createParentDirectories(targetPath);
        try (InputStream inputStream = request.openStream()) {
            Files.copy(inputStream, targetPath);
            validateWrittenSize(targetPath, request.getSize(), normalizedKey);
            return buildUploadedFileInfo(normalizedKey, targetPath, request);
        } catch (FileAlreadyExistsException exception) {
            throw new StorageConflictException("Storage object already exists: " + normalizedKey, exception);
        } catch (IOException exception) {
            throw new StorageBackendException("Failed to upload object to local storage: " + normalizedKey, exception);
        }
    }

    private UploadedFileInfo writeAppendSuffix(String normalizedKey, FileUploadRequest request) throws IOException {
        for (int suffix = 0; suffix < MAX_SUFFIX_RETRIES; suffix++) {
            String candidateKey = suffix == 0
                    ? normalizedKey
                    : StorageObjectKeyGenerator.appendNumericSuffix(normalizedKey, suffix);
            Path candidatePath = resolvePath(candidateKey);
            rejectNonRegularExistingTarget(candidatePath, candidateKey);
            createParentDirectories(candidatePath);
            try (InputStream inputStream = request.openStream()) {
                try (var outputStream = Files.newOutputStream(candidatePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    inputStream.transferTo(outputStream);
                }
                validateWrittenSize(candidatePath, request.getSize(), candidateKey);
                return buildUploadedFileInfo(candidateKey, candidatePath, request);
            } catch (FileAlreadyExistsException exception) {
                // try next suffix
            } catch (IOException exception) {
                throw new StorageBackendException("Failed to upload object to local storage: " + candidateKey, exception);
            }
        }
        throw new StorageBackendException("Failed to find available key after " + MAX_SUFFIX_RETRIES + " attempts: " + normalizedKey);
    }

    private UploadedFileInfo buildUploadedFileInfo(String key, Path targetPath, FileUploadRequest request) {
        return UploadedFileInfo.builder()
                .storageType(StorageType.LOCAL)
                .bucket(null)
                .key(key)
                .originalFilename(request.getOriginalFilename())
                .storedFilename(targetPath.getFileName().toString())
                .contentType(request.getContentType())
                .size(request.getSize())
                .accessUrl(buildAccessUrl(key))
                .build();
    }

    private void createParentDirectories(Path targetPath) throws IOException {
        Path parent = targetPath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new StorageBackendException("Failed to create local storage directories: " + parent, exception);
        }
    }

    private StorageConflictStrategy resolveConflictStrategy(StorageConflictStrategy conflictStrategy) {
        return conflictStrategy == null ? StorageConflictStrategy.APPEND_SUFFIX : conflictStrategy;
    }

    private void validateWrittenSize(Path targetPath, long expectedSize, String key) throws IOException {
        if (expectedSize <= 0) {
            return;
        }
        long actualSize = Files.size(targetPath);
        if (actualSize != expectedSize) {
            Files.deleteIfExists(targetPath);
            throw new StorageBackendException("Upload size mismatch for key " + key
                    + ": expected=" + expectedSize + ", actual=" + actualSize);
        }
    }

    private void cleanupEmptyParentDirectories(Path directory) {
        if (directory == null) {
            return;
        }
        Path current = directory.toAbsolutePath().normalize();
        Path root = rootPath.toAbsolutePath().normalize();
        while (current != null && !current.equals(root) && current.startsWith(root)) {
            try {
                boolean isEmpty;
                try (var stream = Files.list(current)) {
                    isEmpty = stream.findAny().isEmpty();
                }
                if (Files.isDirectory(current) && isEmpty) {
                    Files.delete(current);
                    current = current.getParent();
                } else {
                    break;
                }
            } catch (IOException exception) {
                break;
            }
        }
    }
}
