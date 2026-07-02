package com.zynboot.infra.storage;

import com.zynboot.infra.storage.config.StorageConflictStrategy;
import com.zynboot.infra.storage.config.StorageFilenameStrategy;
import com.zynboot.infra.storage.config.StorageProperties;
import com.zynboot.infra.storage.config.StorageType;
import com.zynboot.infra.storage.exception.StorageConflictException;
import com.zynboot.infra.storage.exception.StorageObjectNotFoundException;
import com.zynboot.infra.storage.model.FileUploadRequest;
import com.zynboot.infra.storage.model.StoredObjectMetadata;
import com.zynboot.infra.storage.model.StoredObject;
import com.zynboot.infra.storage.model.UploadedFileInfo;
import com.zynboot.infra.storage.service.StorageService;
import com.zynboot.infra.storage.service.impl.DefaultStorageService;
import com.zynboot.infra.storage.spi.impl.LocalStorageBackend;
import com.zynboot.infra.storage.support.StorageObjectKeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageBackendTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadAndDeleteThroughStorageService() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setFilenameStrategy(StorageFilenameStrategy.ORIGINAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        UploadedFileInfo uploadedFileInfo = storageService.upload(FileUploadRequest.builder()
                .originalFilename("report 2026.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        Path storedFile = tempDir.resolve(uploadedFileInfo.getKey());
        assertEquals(StorageType.LOCAL, uploadedFileInfo.getStorageType());
        assertTrue(Files.exists(storedFile));
        assertTrue(uploadedFileInfo.getAccessUrl().endsWith(uploadedFileInfo.getKey()));

        storageService.delete(uploadedFileInfo.getKey());

        assertFalse(Files.exists(storedFile));
    }

    @Test
    void shouldAppendSuffixWhenOriginalFilenameAlreadyExists() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setFilenameStrategy(StorageFilenameStrategy.ORIGINAL);
        properties.setConflictStrategy(StorageConflictStrategy.APPEND_SUFFIX);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        UploadedFileInfo first = storageService.upload(FileUploadRequest.builder()
                .originalFilename("report.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());
        UploadedFileInfo second = storageService.upload(FileUploadRequest.builder()
                .originalFilename("report.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        assertTrue(first.getKey().endsWith("/report.txt"));
        assertTrue(second.getKey().endsWith("/report_1.txt"));
        assertTrue(storageService.exists(first.getKey()));
        assertTrue(storageService.exists(second.getKey()));
    }

    @Test
    void shouldFailWhenConfiguredToRejectConflicts() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setFilenameStrategy(StorageFilenameStrategy.ORIGINAL);
        properties.setConflictStrategy(StorageConflictStrategy.FAIL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        storageService.upload(FileUploadRequest.builder()
                .originalFilename("report.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        assertThrows(StorageConflictException.class, () -> storageService.upload(FileUploadRequest.builder()
                .originalFilename("report.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build()));
    }

    @Test
    void shouldUploadToDirCopyMoveAndDeleteBatch() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setFilenameStrategy(StorageFilenameStrategy.ORIGINAL);
        properties.setConflictStrategy(StorageConflictStrategy.APPEND_SUFFIX);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        UploadedFileInfo uploadedFileInfo = storageService.uploadToDir("avatars/user-1", FileUploadRequest.builder()
                .originalFilename("profile.png")
                .contentType("image/png")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        assertTrue(uploadedFileInfo.getKey().matches("avatars/user-1/\\d{4}/\\d{2}/\\d{2}/profile\\.png"),
                "Expected key matching 'avatars/user-1/yyyy/MM/dd/profile.png' but was: " + uploadedFileInfo.getKey());

        String copiedKey = storageService.copy(uploadedFileInfo.getKey(), "archive/profile.png");
        assertEquals("archive/profile.png", copiedKey);
        assertTrue(storageService.exists(copiedKey));

        String movedKey = storageService.move(copiedKey, "archive/final/profile.png");
        assertEquals("archive/final/profile.png", movedKey);
        assertFalse(storageService.exists(copiedKey));
        assertTrue(storageService.exists(movedKey));

        var result = storageService.deleteBatch(List.of(uploadedFileInfo.getKey(), movedKey, " "));
        assertEquals(3, result.getRequestedCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(3, result.getItems().size());
        assertEquals(uploadedFileInfo.getKey(), result.getItems().get(0).getRequestedKey());
        assertTrue(result.getItems().get(0).isSuccess());
        assertEquals(movedKey, result.getItems().get(1).getRequestedKey());
        assertTrue(result.getItems().get(1).isSuccess());
        assertEquals(" ", result.getItems().get(2).getRequestedKey());
        assertFalse(result.getItems().get(2).isSuccess());
        assertEquals("key must not be blank", result.getItems().get(2).getMessage());
        assertFalse(storageService.exists(uploadedFileInfo.getKey()));
        assertFalse(storageService.exists(movedKey));
    }

    @Test
    void shouldKeepExplicitKeyDeterministicAndEncodeAccessUrl() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setConflictStrategy(StorageConflictStrategy.APPEND_SUFFIX);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        UploadedFileInfo first = storageService.upload("docs/test file.txt", FileUploadRequest.builder()
                .originalFilename("ignored.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());
        UploadedFileInfo second = storageService.upload("docs/test file.txt", FileUploadRequest.builder()
                .originalFilename("ignored.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        assertEquals("docs/test file.txt", first.getKey());
        assertEquals("docs/test file_1.txt", second.getKey());
        assertTrue(storageService.exists("docs/test file.txt"));
        assertTrue(storageService.exists("docs/test file_1.txt"));
        assertEquals("/uploads/docs/test%20file.txt", storageService.getAccessUrl("docs/test file.txt"));
    }

    @Test
    void shouldRejectNormalizedBlankKeys() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> storageService.upload("/", FileUploadRequest.builder()
                .originalFilename("a.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build()));
        assertThrows(IllegalArgumentException.class, () -> storageService.delete("//"));
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void shouldRejectDotSegmentsInObjectKeys() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> storageService.upload("docs/../a.txt", FileUploadRequest.builder()
                .originalFilename("a.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build()));
    }

    @Test
    void shouldApplyConflictStrategyForExplicitKeyWhenConfiguredToFail() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setConflictStrategy(StorageConflictStrategy.FAIL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        storageService.upload("docs/fixed.txt", FileUploadRequest.builder()
                .originalFilename("ignored.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        assertThrows(StorageConflictException.class, () -> storageService.upload("docs/fixed.txt", FileUploadRequest.builder()
                .originalFilename("ignored.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build()));
    }

    @Test
    void shouldTreatMoveToSameKeyAsNoOp() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        storageService.upload("docs/a.txt", FileUploadRequest.builder()
                .originalFilename("a.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        assertEquals("docs/a.txt", storageService.move("docs/a.txt", "docs/a.txt"));
        assertThrows(IllegalArgumentException.class, () -> storageService.copy("docs/a.txt", "docs/a.txt"));
    }

    @Test
    void shouldTreatDirectoryAsMissingObject() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);
        Files.createDirectories(tempDir.resolve("docs/folder"));

        assertFalse(storageService.exists("docs/folder"));
        assertThrows(StorageObjectNotFoundException.class, () -> storageService.getMetadata("docs/folder"));
        assertThrows(StorageObjectNotFoundException.class, () -> storageService.open("docs/folder"));
        assertThrows(StorageObjectNotFoundException.class, () -> storageService.delete("docs/folder"));
        assertTrue(Files.isDirectory(tempDir.resolve("docs/folder")));
    }

    @Test
    void shouldThrowNotFoundForMissingLocalObject() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        assertThrows(StorageObjectNotFoundException.class, () -> storageService.getMetadata("docs/missing.txt"));
        assertThrows(StorageObjectNotFoundException.class, () -> storageService.open("docs/missing.txt"));
    }

    @Test
    void shouldOpenAndReadStoredObjectContent() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
        storageService.upload("docs/readme.txt", FileUploadRequest.builder()
                .originalFilename("readme.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        try (StoredObject storedObject = storageService.open("docs/readme.txt")) {
            assertEquals("docs/readme.txt", storedObject.key());
            assertEquals(content.length, storedObject.size());
            assertEquals("hello storage", new String(storedObject.inputStream().readAllBytes(), StandardCharsets.UTF_8));
        }

        StoredObjectMetadata metadata = storageService.getMetadata("docs/readme.txt");
        assertEquals("docs/readme.txt", metadata.key());
        assertEquals(content.length, metadata.size());
        assertTrue(metadata.accessUrl().endsWith("docs/readme.txt"));
        assertEquals("hello storage", new String(storageService.readBytes("docs/readme.txt"), StandardCharsets.UTF_8));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertEquals(content.length, storageService.transferTo("docs/readme.txt", outputStream));
        assertEquals("hello storage", outputStream.toString(StandardCharsets.UTF_8));

        Path downloadPath = tempDir.resolve("downloads/readme.txt");
        assertEquals(downloadPath.toAbsolutePath().normalize(), storageService.downloadTo("docs/readme.txt", downloadPath));
        assertEquals("hello storage", Files.readString(downloadPath, StandardCharsets.UTF_8));
    }

    @Test
    void shouldRejectReadBytesWhenObjectExceedsInMemoryThreshold() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setType(StorageType.LOCAL);
        properties.setMaxInMemoryReadBytes(4);
        properties.getLocal().setRootPath(tempDir.toString());

        StorageService storageService = createStorageService(properties);

        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        storageService.upload("docs/large.txt", FileUploadRequest.builder()
                .originalFilename("large.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new java.io.ByteArrayInputStream(content))
                .build());

        IOException exception = assertThrows(IOException.class, () -> storageService.readBytes("docs/large.txt"));
        assertTrue(exception.getMessage().contains("too large"));
    }

    private StorageService createStorageService(StorageProperties properties) throws Exception {
        return new DefaultStorageService(
                new LocalStorageBackend(properties),
                new StorageObjectKeyGenerator(properties.getDatePathPattern(), properties.getFilenameStrategy()),
                properties.getConflictStrategy(),
                properties.getMaxInMemoryReadBytes()
        );
    }
}
