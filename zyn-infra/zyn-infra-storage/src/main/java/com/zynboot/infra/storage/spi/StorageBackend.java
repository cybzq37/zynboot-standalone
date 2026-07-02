package com.zynboot.infra.storage.spi;

import com.zynboot.infra.storage.config.StorageConflictStrategy;
import com.zynboot.infra.storage.model.FileUploadRequest;
import com.zynboot.infra.storage.model.BatchDeleteResult;
import com.zynboot.infra.storage.model.StoredObject;
import com.zynboot.infra.storage.model.StoredObjectMetadata;
import com.zynboot.infra.storage.model.UploadedFileInfo;

import java.io.IOException;
import java.util.Collection;

public interface StorageBackend {

    UploadedFileInfo upload(String key, FileUploadRequest request, StorageConflictStrategy conflictStrategy) throws IOException;

    StoredObject open(String key) throws IOException;

    StoredObjectMetadata getMetadata(String key) throws IOException;

    boolean exists(String key) throws IOException;

    void copy(String sourceKey, String targetKey) throws IOException;

    void move(String sourceKey, String targetKey) throws IOException;

    String getAccessUrl(String key);

    BatchDeleteResult deleteBatch(Collection<String> keys);

    void delete(String key) throws IOException;
}
