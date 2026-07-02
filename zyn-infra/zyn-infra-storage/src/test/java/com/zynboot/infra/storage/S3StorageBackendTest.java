package com.zynboot.infra.storage;

import com.zynboot.infra.storage.config.StorageConflictStrategy;
import com.zynboot.infra.storage.config.StorageProperties;
import com.zynboot.infra.storage.exception.StorageConflictException;
import com.zynboot.infra.storage.exception.StorageObjectNotFoundException;
import com.zynboot.infra.storage.model.FileUploadRequest;
import com.zynboot.infra.storage.model.StoredObject;
import com.zynboot.infra.storage.model.StoredObjectMetadata;
import com.zynboot.infra.storage.model.UploadedFileInfo;
import com.zynboot.infra.storage.spi.impl.S3StorageBackend;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3StorageBackendTest {

    @Test
    void shouldBuildPathStyleAccessUrlFromEndpoint() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getS3().setBucket("files");
        properties.getS3().setEndpoint("http://localhost:9000");
        properties.getS3().setPathStyleAccess(true);

        S3StorageBackend backend = new S3StorageBackend(properties, stubS3Client(), false);
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        UploadedFileInfo uploadedFileInfo = backend.upload("docs/readme.txt", FileUploadRequest.builder()
                .originalFilename("readme.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new ByteArrayInputStream(content))
                .build(), StorageConflictStrategy.OVERWRITE);

        assertEquals("http://localhost:9000/files/docs/readme.txt", uploadedFileInfo.getAccessUrl());
    }

    @Test
    void shouldBuildVirtualHostStyleAccessUrlFromEndpoint() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getS3().setBucket("files");
        properties.getS3().setEndpoint("http://localhost:9000");
        properties.getS3().setPathStyleAccess(false);

        S3StorageBackend backend = new S3StorageBackend(properties, stubS3Client(), false);
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        UploadedFileInfo uploadedFileInfo = backend.upload("docs/readme.txt", FileUploadRequest.builder()
                .originalFilename("readme.txt")
                .contentType("text/plain")
                .size(content.length)
                .inputStreamSource(() -> new ByteArrayInputStream(content))
                .build(), StorageConflictStrategy.OVERWRITE);

        assertEquals("http://files.localhost:9000/docs/readme.txt", uploadedFileInfo.getAccessUrl());
    }

    @Test
    void shouldCheckExistsThroughHeadObject() throws Exception {
        Set<String> existingKeys = new HashSet<>();
        existingKeys.add("docs/readme.txt");

        S3StorageBackend backend = new S3StorageBackend(stubProperties(), stubS3Client(existingKeys), false);

        assertTrue(backend.exists("docs/readme.txt"));
        assertFalse(backend.exists("docs/missing.txt"));
    }

    @Test
    void shouldRejectInvalidS3ObjectKeyAndThrowNotFoundConsistently() {
        S3StorageBackend backend = new S3StorageBackend(stubProperties(), stubS3Client(), false);

        assertThrows(IllegalArgumentException.class, () -> backend.getAccessUrl("docs/../bad.txt"));
        assertThrows(StorageObjectNotFoundException.class, () -> backend.getMetadata("docs/missing.txt"));
        assertThrows(StorageObjectNotFoundException.class, () -> backend.open("docs/missing.txt"));
    }

    @Test
    void shouldApplyFailStrategyAtomicallyForS3Upload() {
        Set<String> existingKeys = new HashSet<>();
        existingKeys.add("docs/readme.txt");
        S3StorageBackend backend = new S3StorageBackend(stubProperties(), stubS3Client(existingKeys), false);
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(StorageConflictException.class, () -> backend.upload(
                "docs/readme.txt",
                FileUploadRequest.builder()
                        .originalFilename("readme.txt")
                        .contentType("text/plain")
                        .size(content.length)
                        .inputStreamSource(() -> new ByteArrayInputStream(content))
                        .build(),
                StorageConflictStrategy.FAIL
        ));
    }

    @Test
    void shouldCopyObjectWithinBucket() throws Exception {
        Set<String> copiedTargets = new HashSet<>();
        S3StorageBackend backend = new S3StorageBackend(stubProperties(), stubS3Client(new HashSet<>(), copiedTargets), false);

        backend.copy("docs/readme.txt", "archive/readme.txt");

        assertTrue(copiedTargets.contains("archive/readme.txt"));
        assertEquals("https://files.s3.cn-north-1.amazonaws.com/archive/readme.txt", backend.getAccessUrl("archive/readme.txt"));
    }

    @Test
    void shouldEncodeAccessUrlForExplicitKey() {
        S3StorageBackend backend = new S3StorageBackend(stubProperties(), stubS3Client(), false);

        assertEquals("https://files.s3.cn-north-1.amazonaws.com/docs/%E6%B5%8B%E8%AF%95%20file.txt",
                backend.getAccessUrl("docs/测试 file.txt"));
    }

    @Test
    void shouldOpenStoredObjectAndDeleteBatch() throws Exception {
        Set<String> existingKeys = new HashSet<>();
        existingKeys.add("docs/readme.txt");
        existingKeys.add("docs/remove.txt");
        S3StorageBackend backend = new S3StorageBackend(stubProperties(), stubS3Client(existingKeys, new HashSet<>()), false);

        try (StoredObject storedObject = backend.open("docs/readme.txt")) {
            assertEquals("docs/readme.txt", storedObject.key());
            assertEquals(5, storedObject.size());
            assertEquals("text/plain", storedObject.contentType());
            assertEquals("hello", new String(storedObject.inputStream().readAllBytes(), StandardCharsets.UTF_8));
        }

        StoredObjectMetadata metadata = backend.getMetadata("docs/readme.txt");
        assertEquals("docs/readme.txt", metadata.key());
        assertEquals(5, metadata.size());
        assertEquals("text/plain", metadata.contentType());

        var result = backend.deleteBatch(List.of("docs/readme.txt", "docs/remove.txt", "docs/readme.txt"));
        assertEquals(3, result.getRequestedCount());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(3, result.getItems().size());
        assertEquals("docs/readme.txt", result.getItems().get(0).getRequestedKey());
        assertEquals("docs/remove.txt", result.getItems().get(1).getRequestedKey());
        assertEquals("docs/readme.txt", result.getItems().get(2).getRequestedKey());
        assertTrue(result.getItems().stream().allMatch(item -> item.isSuccess() && item.getMessage() == null));
    }

    @Test
    void shouldRollbackTargetWhenS3MoveDeleteSourceFails() {
        Set<String> existingKeys = new HashSet<>();
        existingKeys.add("docs/readme.txt");
        Set<String> copiedTargets = new HashSet<>();
        Set<String> deletedKeys = new HashSet<>();
        Set<String> failDeleteKeys = Set.of("docs/readme.txt");
        S3StorageBackend backend = new S3StorageBackend(
                stubProperties(),
                stubS3Client(existingKeys, copiedTargets, deletedKeys, failDeleteKeys),
                false
        );

        IOException exception = assertThrows(IOException.class,
                () -> backend.move("docs/readme.txt", "archive/readme.txt"));

        assertTrue(exception.getMessage().contains("rollback completed"));
        assertTrue(existingKeys.contains("docs/readme.txt"));
        assertFalse(existingKeys.contains("archive/readme.txt"));
        assertTrue(copiedTargets.contains("archive/readme.txt"));
        assertTrue(deletedKeys.contains("archive/readme.txt"));
        assertFalse(deletedKeys.contains("docs/readme.txt"));
    }

    private StorageProperties stubProperties() {
        StorageProperties properties = new StorageProperties();
        properties.getS3().setBucket("files");
        return properties;
    }

    private S3Client stubS3Client() {
        return stubS3Client(new HashSet<>(), new HashSet<>(), new HashSet<>(), Set.of());
    }

    private S3Client stubS3Client(Set<String> existingKeys) {
        return stubS3Client(existingKeys, new HashSet<>(), new HashSet<>(), Set.of());
    }

    private S3Client stubS3Client(Set<String> existingKeys, Set<String> copiedTargets) {
        return stubS3Client(existingKeys, copiedTargets, new HashSet<>(), Set.of());
    }

    private S3Client stubS3Client(Set<String> existingKeys,
                                  Set<String> copiedTargets,
                                  Set<String> deletedKeys,
                                  Set<String> failDeleteKeys) {
        return (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class[]{S3Client.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "putObject" -> handlePutObject(existingKeys, args[0]);
                    case "copyObject" -> handleCopyObject(existingKeys, copiedTargets, (CopyObjectRequest) args[0]);
                    case "getObject" -> handleGetObject(existingKeys, (GetObjectRequest) args[0]);
                    case "deleteObjects" -> handleDeleteObjects(existingKeys, (DeleteObjectsRequest) args[0]);
                    case "deleteObject" -> handleDeleteObject(existingKeys, deletedKeys, failDeleteKeys, args[0]);
                    case "headObject" -> handleHeadObject(existingKeys, (HeadObjectRequest) args[0]);
                    case "close" -> null;
                    case "serviceName" -> "s3";
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "StubS3Client";
                    default -> throw new UnsupportedOperationException("Unsupported method: " + method.getName());
                }
        );
    }

    private HeadObjectResponse handleHeadObject(Set<String> existingKeys, HeadObjectRequest request) {
        if (existingKeys.contains(request.key())) {
            return HeadObjectResponse.builder()
                    .contentLength(5L)
                    .contentType("text/plain")
                    .build();
        }
        throw S3Exception.builder().statusCode(404).message("Not Found").build();
    }

    private PutObjectResponse handlePutObject(Set<String> existingKeys, Object argument) {
        var request = (software.amazon.awssdk.services.s3.model.PutObjectRequest) argument;
        existingKeys.add(request.key());
        return PutObjectResponse.builder().build();
    }

    private Object handleCopyObject(Set<String> existingKeys, Set<String> copiedTargets, CopyObjectRequest request) {
        existingKeys.add(request.destinationKey());
        copiedTargets.add(request.destinationKey());
        return null;
    }

    private DeleteObjectResponse handleDeleteObject(Set<String> existingKeys,
                                                    Set<String> deletedKeys,
                                                    Set<String> failDeleteKeys,
                                                    Object argument) {
        var request = (software.amazon.awssdk.services.s3.model.DeleteObjectRequest) argument;
        if (failDeleteKeys.contains(request.key())) {
            throw S3Exception.builder().statusCode(500).message("Delete Failed").build();
        }
        existingKeys.remove(request.key());
        deletedKeys.add(request.key());
        return DeleteObjectResponse.builder().build();
    }

    private ResponseInputStream<GetObjectResponse> handleGetObject(Set<String> existingKeys, GetObjectRequest request) {
        if (!existingKeys.contains(request.key())) {
            throw S3Exception.builder().statusCode(404).message("Not Found").build();
        }
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("text/plain")
                .contentLength((long) bytes.length)
                .build();
        return new ResponseInputStream<>(response, AbortableInputStream.create(new ByteArrayInputStream(bytes)));
    }

    private DeleteObjectsResponse handleDeleteObjects(Set<String> existingKeys, DeleteObjectsRequest request) {
        var deleted = new ArrayList<DeletedObject>();
        Delete delete = request.delete();
        for (ObjectIdentifier objectIdentifier : delete.objects()) {
            existingKeys.remove(objectIdentifier.key());
            deleted.add(DeletedObject.builder().key(objectIdentifier.key()).build());
        }
        return DeleteObjectsResponse.builder()
                .deleted(deleted)
                .build();
    }
}
