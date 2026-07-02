package com.zynboot.infra.storage.service.impl;

import com.zynboot.infra.storage.config.StorageConflictStrategy;
import com.zynboot.infra.storage.model.BatchDeleteItemResult;
import com.zynboot.infra.storage.model.BatchDeleteResult;
import com.zynboot.infra.storage.model.FileUploadRequest;
import com.zynboot.infra.storage.model.StoredObject;
import com.zynboot.infra.storage.model.StoredObjectMetadata;
import com.zynboot.infra.storage.model.UploadedFileInfo;
import com.zynboot.infra.storage.service.StorageService;
import com.zynboot.infra.storage.spi.StorageBackend;
import com.zynboot.infra.storage.support.StorageObjectKeyGenerator;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultStorageService implements StorageService {

    private final StorageBackend backend;
    private final StorageObjectKeyGenerator keyGenerator;
    private final StorageConflictStrategy conflictStrategy;
    private final long maxInMemoryReadBytes;

    public DefaultStorageService(StorageBackend backend,
                                 StorageObjectKeyGenerator keyGenerator,
                                 StorageConflictStrategy conflictStrategy,
                                 long maxInMemoryReadBytes) {
        this.backend = backend;
        this.keyGenerator = keyGenerator;
        this.conflictStrategy = conflictStrategy == null ? StorageConflictStrategy.APPEND_SUFFIX : conflictStrategy;
        this.maxInMemoryReadBytes = maxInMemoryReadBytes;
    }

    @Override
    public UploadedFileInfo upload(FileUploadRequest request) throws IOException {
        Assert.notNull(request, "request must not be null");
        request.validate();
        return uploadGeneratedKey(keyGenerator.generate(request.getOriginalFilename()), request);
    }

    @Override
    public UploadedFileInfo upload(String key, FileUploadRequest request) throws IOException {
        Assert.notNull(request, "request must not be null");
        request.validate();
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        return backend.upload(normalizedKey, request, conflictStrategy);
    }

    @Override
    public UploadedFileInfo uploadToDir(String dir, FileUploadRequest request) throws IOException {
        Assert.notNull(request, "request must not be null");
        request.validate();
        String normalizedDir = StorageObjectKeyGenerator.requireValidPrefix(dir, "dir");
        return uploadGeneratedKey(keyGenerator.generateUnderPrefix(normalizedDir, request.getOriginalFilename()), request);
    }

    @Override
    public StoredObject open(String key) throws IOException {
        return backend.open(StorageObjectKeyGenerator.requireValidKey(key, "key"));
    }

    @Override
    public StoredObjectMetadata getMetadata(String key) throws IOException {
        return backend.getMetadata(StorageObjectKeyGenerator.requireValidKey(key, "key"));
    }

    @Override
    public byte[] readBytes(String key) throws IOException {
        try (StoredObject storedObject = open(key)) {
            if (storedObject.size() > maxInMemoryReadBytes) {
                throw new IOException("Stored object is too large to read into memory: " + storedObject.key());
            }
            return readBytesWithLimit(storedObject);
        }
    }

    @Override
    public long transferTo(String key, OutputStream outputStream) throws IOException {
        Assert.notNull(outputStream, "outputStream must not be null");
        try (StoredObject storedObject = open(key)) {
            return storedObject.inputStream().transferTo(outputStream);
        }
    }

    @Override
    public Path downloadTo(String key, Path targetPath) throws IOException {
        Assert.notNull(targetPath, "targetPath must not be null");
        Path absoluteTargetPath = targetPath.toAbsolutePath().normalize();
        Path parent = absoluteTargetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(absoluteTargetPath)) {
            transferTo(key, outputStream);
        }
        return absoluteTargetPath;
    }

    @Override
    public boolean exists(String key) throws IOException {
        return backend.exists(StorageObjectKeyGenerator.requireValidKey(key, "key"));
    }

    @Override
    public String getAccessUrl(String key) {
        return backend.getAccessUrl(StorageObjectKeyGenerator.requireValidKey(key, "key"));
    }

    @Override
    public void delete(String key) throws IOException {
        backend.delete(StorageObjectKeyGenerator.requireValidKey(key, "key"));
    }

    @Override
    public BatchDeleteResult deleteBatch(Collection<String> keys) {
        Assert.notNull(keys, "keys must not be null");
        List<String> requestedKeys = new ArrayList<>(keys);

        List<String> validKeys = new ArrayList<>(requestedKeys.size());
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (String key : requestedKeys) {
            try {
                validKeys.add(StorageObjectKeyGenerator.requireValidKey(key, "key"));
            } catch (IllegalArgumentException exception) {
                validationErrors.put(key, exception.getMessage());
            }
        }

        Map<String, BatchDeleteItemResult> backendResultsByKey = new LinkedHashMap<>();
        if (!validKeys.isEmpty()) {
            BatchDeleteResult backendResult = backend.deleteBatch(validKeys);
            List<BatchDeleteItemResult> backendItems = backendResult.getItems() == null ? List.of() : backendResult.getItems();
            for (BatchDeleteItemResult item : backendItems) {
                backendResultsByKey.put(item.getRequestedKey(), item);
            }
        }

        BatchDeleteResult.BatchDeleteResultBuilder builder = BatchDeleteResult.builder()
                .requestedCount(requestedKeys.size());
        int successCount = 0;
        int failureCount = 0;
        int validIndex = 0;
        for (String requestedKey : requestedKeys) {
            String validationError = validationErrors.get(requestedKey);
            if (validationError != null) {
                builder.item(BatchDeleteItemResult.builder()
                        .requestedKey(requestedKey)
                        .resolvedKey(null)
                        .success(false)
                        .message(validationError)
                        .build());
                failureCount++;
            } else {
                String normalizedKey = validKeys.get(validIndex);
                validIndex++;
                BatchDeleteItemResult backendItem = backendResultsByKey.get(normalizedKey);
                if (backendItem != null) {
                    builder.item(BatchDeleteItemResult.builder()
                            .requestedKey(requestedKey)
                            .resolvedKey(backendItem.getResolvedKey())
                            .success(backendItem.isSuccess())
                            .message(backendItem.getMessage())
                            .build());
                    if (backendItem.isSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } else {
                    builder.item(BatchDeleteItemResult.builder()
                            .requestedKey(requestedKey)
                            .resolvedKey(normalizedKey)
                            .success(false)
                            .message("Unknown delete result")
                            .build());
                    failureCount++;
                }
            }
        }
        return builder.successCount(successCount)
                .failureCount(failureCount)
                .build();
    }

    @Override
    public String copy(String sourceKey, String targetKey) throws IOException {
        String normalizedSourceKey = StorageObjectKeyGenerator.requireValidKey(sourceKey, "sourceKey");
        String normalizedTargetKey = StorageObjectKeyGenerator.requireValidKey(targetKey, "targetKey");
        if (normalizedSourceKey.equals(normalizedTargetKey)) {
            throw new IllegalArgumentException("sourceKey and targetKey must not resolve to the same key");
        }
        backend.copy(normalizedSourceKey, normalizedTargetKey);
        return normalizedTargetKey;
    }

    @Override
    public String move(String sourceKey, String targetKey) throws IOException {
        String normalizedSourceKey = StorageObjectKeyGenerator.requireValidKey(sourceKey, "sourceKey");
        String normalizedTargetKey = StorageObjectKeyGenerator.requireValidKey(targetKey, "targetKey");
        if (normalizedSourceKey.equals(normalizedTargetKey)) {
            return normalizedSourceKey;
        }
        backend.move(normalizedSourceKey, normalizedTargetKey);
        return normalizedTargetKey;
    }

    private UploadedFileInfo uploadGeneratedKey(String generatedKey, FileUploadRequest request) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(generatedKey, "key");
        return backend.upload(normalizedKey, request, conflictStrategy);
    }

    private byte[] readBytesWithLimit(StoredObject storedObject) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long totalRead = 0;
        int read;
        while ((read = storedObject.inputStream().read(buffer)) != -1) {
            totalRead += read;
            if (totalRead > maxInMemoryReadBytes) {
                throw new IOException("Stored object is too large to read into memory: " + storedObject.key());
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }


}
