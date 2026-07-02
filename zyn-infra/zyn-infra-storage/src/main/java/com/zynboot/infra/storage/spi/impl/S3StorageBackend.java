package com.zynboot.infra.storage.spi.impl;

import com.zynboot.infra.storage.model.BatchDeleteResult;
import com.zynboot.infra.storage.model.BatchDeleteItemResult;
import com.zynboot.infra.storage.config.StorageConflictStrategy;
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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class S3StorageBackend implements StorageBackend, AutoCloseable {

    private static final int MAX_SUFFIX_RETRIES = 1000;
    private static final long MULTIPART_THRESHOLD = 100 * 1024 * 1024L;
    private static final long PART_SIZE = 8 * 1024 * 1024L;

    private final StorageProperties properties;
    private final StorageProperties.S3Properties s3Properties;
    private final S3Client s3Client;
    private final boolean closeClient;

    public S3StorageBackend(StorageProperties properties, S3Client s3Client, boolean closeClient) {
        this.properties = properties;
        this.s3Properties = properties.getS3();
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client must not be null");
        this.closeClient = closeClient;
    }

    @Override
    public UploadedFileInfo upload(String key,
                                   FileUploadRequest request,
                                   StorageConflictStrategy conflictStrategy) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        return switch (resolveConflictStrategy(conflictStrategy)) {
            case OVERWRITE -> putObject(normalizedKey, request, false);
            case FAIL -> putObject(normalizedKey, request, true);
            case APPEND_SUFFIX -> putObjectWithSuffix(normalizedKey, request);
        };
    }

    @Override
    public StoredObject open(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        try {
            ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(normalizedKey)
                    .build());
            GetObjectResponse response = inputStream.response();
            long size = response.contentLength() == null ? -1L : response.contentLength();
            String contentType = StringUtils.hasText(response.contentType()) ? response.contentType() : "application/octet-stream";
            return new StoredObject(normalizedKey, contentType, size, inputStream);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new StorageObjectNotFoundException("Storage object not found: " + normalizedKey, exception);
            }
            throw new StorageBackendException("Failed to open object from S3: " + normalizedKey, exception);
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to open object from S3: " + normalizedKey, exception);
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(normalizedKey)
                    .build());
            long size = response.contentLength() == null ? -1L : response.contentLength();
            String contentType = StringUtils.hasText(response.contentType()) ? response.contentType() : "application/octet-stream";
            return new StoredObjectMetadata(normalizedKey, contentType, size, buildAccessUrl(normalizedKey));
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new StorageObjectNotFoundException("Storage object not found: " + normalizedKey, exception);
            }
            throw new StorageBackendException("Failed to load object metadata from S3: " + normalizedKey, exception);
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to load object metadata from S3: " + normalizedKey, exception);
        }
    }

    @Override
    public boolean exists(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(normalizedKey)
                    .build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw new StorageBackendException("Failed to query object from S3: " + normalizedKey, exception);
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to query object from S3: " + normalizedKey, exception);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey) throws IOException {
        String normalizedSourceKey = StorageObjectKeyGenerator.requireValidKey(sourceKey, "sourceKey");
        String normalizedTargetKey = StorageObjectKeyGenerator.requireValidKey(targetKey, "targetKey");
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .destinationBucket(s3Properties.getBucket())
                    .destinationKey(normalizedTargetKey)
                    .copySource(encodeCopySource(s3Properties.getBucket(), normalizedSourceKey))
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new StorageObjectNotFoundException("Storage object not found: " + normalizedSourceKey, exception);
            }
            throw new StorageBackendException("Failed to copy object in S3: " + normalizedSourceKey + " -> " + normalizedTargetKey, exception);
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to copy object in S3: " + normalizedSourceKey + " -> " + normalizedTargetKey, exception);
        }
    }

    @Override
    public void move(String sourceKey, String targetKey) throws IOException {
        String normalizedSourceKey = StorageObjectKeyGenerator.requireValidKey(sourceKey, "sourceKey");
        String normalizedTargetKey = StorageObjectKeyGenerator.requireValidKey(targetKey, "targetKey");
        copy(normalizedSourceKey, normalizedTargetKey);
        try {
            delete(normalizedSourceKey);
        } catch (StorageObjectNotFoundException deleteException) {
            throw deleteException;
        } catch (IOException deleteException) {
            try {
                delete(normalizedTargetKey);
            } catch (IOException rollbackException) {
                deleteException.addSuppressed(rollbackException);
                throw new StorageBackendException("Failed to move object in S3 and rollback target failed: "
                        + normalizedSourceKey + " -> " + normalizedTargetKey, deleteException);
            }
            throw new StorageBackendException("Failed to move object in S3, target rollback completed: "
                    + normalizedSourceKey + " -> " + normalizedTargetKey, deleteException);
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
        if (keys.isEmpty()) {
            builder.successCount(0);
            builder.failureCount(0);
            return builder.build();
        }
        List<String> normalizedKeys = keys.stream()
                .map(key -> StorageObjectKeyGenerator.requireValidKey(key, "key"))
                .toList();

        List<String> uniqueKeys = normalizedKeys.stream().distinct().toList();
        List<ObjectIdentifier> identifiers = uniqueKeys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

        Map<String, BatchDeleteItemResult> resultByKey = new HashMap<>();
        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .delete(Delete.builder().objects(identifiers).build())
                    .build());
            List<DeletedObject> deletedObjects = response.deleted() == null ? List.of() : response.deleted();
            List<S3Error> errors = response.errors() == null ? List.of() : response.errors();

            Set<String> deletedKeys = new HashSet<>();
            for (DeletedObject deleted : deletedObjects) {
                deletedKeys.add(deleted.key());
            }
            Map<String, String> errorByKey = new HashMap<>();
            for (S3Error error : errors) {
                errorByKey.putIfAbsent(error.key(), error.message());
            }

            for (String key : uniqueKeys) {
                String errorMessage = errorByKey.get(key);
                if (errorMessage != null) {
                    resultByKey.put(key, BatchDeleteItemResult.builder()
                            .requestedKey(key)
                            .resolvedKey(key)
                            .success(false)
                            .message(errorMessage)
                            .build());
                } else if (deletedKeys.contains(key)) {
                    resultByKey.put(key, BatchDeleteItemResult.builder()
                            .requestedKey(key)
                            .resolvedKey(key)
                            .success(true)
                            .build());
                } else {
                    resultByKey.put(key, BatchDeleteItemResult.builder()
                            .requestedKey(key)
                            .resolvedKey(key)
                            .success(false)
                            .message("Unknown delete result")
                            .build());
                }
            }
        } catch (SdkException exception) {
            for (String key : uniqueKeys) {
                resultByKey.put(key, BatchDeleteItemResult.builder()
                        .requestedKey(key)
                        .resolvedKey(key)
                        .success(false)
                        .message(exception.getMessage())
                        .build());
            }
        }

        int successCount = 0;
        int failureCount = 0;
        for (String key : normalizedKeys) {
            BatchDeleteItemResult item = resultByKey.get(key);
            builder.item(item);
            if (item.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        return builder.successCount(successCount)
                .failureCount(failureCount)
                .build();
    }

    @Override
    public void delete(String key) throws IOException {
        String normalizedKey = StorageObjectKeyGenerator.requireValidKey(key, "key");
        try {
            // S3 deleteObject is idempotent — succeeds even if the key does not exist.
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(normalizedKey)
                    .build());
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to delete object from S3: " + normalizedKey, exception);
        }
    }

    @Override
    public void close() {
        if (closeClient) {
            s3Client.close();
        }
    }

    private String buildAccessUrl(String key) {
        String encodedKey = StorageObjectKeyGenerator.encodeUrlPath(key);
        String publicBaseUrl = StorageObjectKeyGenerator.stripTrailingSlash(properties.getPublicBaseUrl());
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl + "/" + encodedKey;
        }

        String domain = StorageObjectKeyGenerator.stripTrailingSlash(s3Properties.getDomain());
        if (StringUtils.hasText(domain)) {
            return domain + "/" + encodedKey;
        }

        String endpoint = StorageObjectKeyGenerator.stripTrailingSlash(s3Properties.getEndpoint());
        if (StringUtils.hasText(endpoint)) {
            return buildEndpointAccessUrl(endpoint, encodedKey);
        }

        String region = StringUtils.hasText(s3Properties.getRegion()) ? s3Properties.getRegion().trim() : "us-east-1";
        return "https://" + s3Properties.getBucket()
                + ".s3."
                + region
                + ".amazonaws.com/"
                + encodedKey;
    }

    private String buildEndpointAccessUrl(String endpoint, String key) {
        URI endpointUri = URI.create(endpoint);
        String normalizedPath = normalizeUriPath(endpointUri.getPath());
        if (s3Properties.isPathStyleAccess() || !StringUtils.hasText(endpointUri.getHost())) {
            return rebuildUri(endpointUri, endpointUri.getHost(), joinPath(normalizedPath, s3Properties.getBucket(), key));
        }
        String bucketHost = s3Properties.getBucket() + "." + endpointUri.getHost();
        return rebuildUri(endpointUri, bucketHost, joinPath(normalizedPath, key));
    }

    private String rebuildUri(URI uri, String host, String path) {
        try {
            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    host,
                    uri.getPort(),
                    path,
                    null,
                    null
            ).toString();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to build storage access URL", exception);
        }
    }

    private String normalizeUriPath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String joinPath(String prefix, String... segments) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(prefix)) {
            builder.append(prefix);
        }
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            if (builder.isEmpty() || builder.charAt(builder.length() - 1) != '/') {
                builder.append('/');
            }
            builder.append(StorageObjectKeyGenerator.normalizeKey(segment));
        }
        return builder.isEmpty() ? "/" : builder.toString();
    }

    private String encodeCopySource(String bucket, String key) {
        String encodedKey = StorageObjectKeyGenerator.encodeUrlPath(key);
        return bucket + "/" + encodedKey;
    }




    private UploadedFileInfo putObject(String key,
                                       FileUploadRequest request,
                                       boolean failIfExists) throws IOException {
        if (failIfExists && exists(key)) {
            throw new StorageConflictException("Storage object already exists: " + key);
        }
        if (request.getSize() > MULTIPART_THRESHOLD) {
            return putObjectMultipart(key, request);
        }
        PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key);
        if (StringUtils.hasText(request.getContentType())) {
            putObjectRequest.contentType(request.getContentType());
        }
        try (InputStream inputStream = request.openStream()) {
            s3Client.putObject(putObjectRequest.build(), RequestBody.fromInputStream(inputStream, request.getSize()));
            return buildUploadedFileInfo(key, request);
        } catch (S3Exception exception) {
            throw new StorageBackendException("Failed to upload object to S3: " + key, exception);
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to upload object to S3: " + key, exception);
        }
    }

    private UploadedFileInfo putObjectMultipart(String key, FileUploadRequest request) throws IOException {
        String contentType = StringUtils.hasText(request.getContentType()) ? request.getContentType() : "application/octet-stream";
        CreateMultipartUploadResponse createResponse;
        try {
            createResponse = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build());
        } catch (SdkException exception) {
            throw new StorageBackendException("Failed to initiate multipart upload for S3: " + key, exception);
        }
        String uploadId = createResponse.uploadId();
        List<CompletedPart> completedParts = new ArrayList<>();
        try (InputStream inputStream = request.openStream()) {
            byte[] buffer = new byte[(int) PART_SIZE];
            int partNumber = 1;
            long totalRead = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] partData = bytesRead == buffer.length ? buffer : Arrays.copyOf(buffer, bytesRead);
                UploadPartResponse partResponse;
                try {
                    partResponse = s3Client.uploadPart(UploadPartRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .contentLength((long) bytesRead)
                            .build(), RequestBody.fromBytes(partData));
                } catch (SdkException exception) {
                    abortMultipartQuietly(key, uploadId);
                    throw new StorageBackendException("Failed to upload part " + partNumber + " for S3 key: " + key, exception);
                }
                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(partResponse.eTag())
                        .build());
                totalRead += bytesRead;
                partNumber++;
            }
            if (request.getSize() > 0 && totalRead != request.getSize()) {
                abortMultipartQuietly(key, uploadId);
                throw new StorageBackendException("Upload size mismatch for key " + key
                        + ": expected=" + request.getSize() + ", actual=" + totalRead);
            }
        } catch (IOException exception) {
            abortMultipartQuietly(key, uploadId);
            throw exception;
        }
        try {
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build());
        } catch (SdkException exception) {
            abortMultipartQuietly(key, uploadId);
            throw new StorageBackendException("Failed to complete multipart upload for S3: " + key, exception);
        }
        return buildUploadedFileInfo(key, request);
    }

    private void abortMultipartQuietly(String key, String uploadId) {
        // best-effort cleanup: if completeMultipartUpload has already succeeded on S3,
        // the abort is a no-op and the object remains. This is an S3 design constraint.
        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .uploadId(uploadId)
                    .build());
        } catch (Exception ignored) {
        }
    }

    private UploadedFileInfo putObjectWithSuffix(String normalizedKey, FileUploadRequest request) throws IOException {
        // NOTE: Unlike LocalStorageBackend which uses CREATE_NEW for atomic conflict detection,
        // S3 PutObject has no conditional-write primitive. The exists() + putObject() sequence
        // has a TOCTOU window where a concurrent write could create the same key. This is an
        // inherent S3 limitation — the conflict would manifest as a silent overwrite rather than
        // an error. Applications requiring strict uniqueness should use UUID-based keys (default).
        for (int suffix = 0; suffix < MAX_SUFFIX_RETRIES; suffix++) {
            String candidateKey = suffix == 0
                    ? normalizedKey
                    : StorageObjectKeyGenerator.appendNumericSuffix(normalizedKey, suffix);
            try {
                return putObject(candidateKey, request, true);
            } catch (StorageConflictException exception) {
                // try next suffix
            }
        }
        throw new StorageBackendException("Failed to find available key after " + MAX_SUFFIX_RETRIES + " attempts: " + normalizedKey);
    }

    private UploadedFileInfo buildUploadedFileInfo(String key, FileUploadRequest request) {
        return UploadedFileInfo.builder()
                .storageType(StorageType.S3)
                .bucket(s3Properties.getBucket())
                .key(key)
                .originalFilename(request.getOriginalFilename())
                .storedFilename(StorageObjectKeyGenerator.extractFilename(key))
                .contentType(request.getContentType())
                .size(request.getSize())
                .accessUrl(buildAccessUrl(key))
                .build();
    }

    private StorageConflictStrategy resolveConflictStrategy(StorageConflictStrategy conflictStrategy) {
        return conflictStrategy == null ? StorageConflictStrategy.APPEND_SUFFIX : conflictStrategy;
    }

}
