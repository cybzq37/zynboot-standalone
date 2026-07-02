package com.zynboot.infra.storage.service;

import com.zynboot.infra.storage.model.BatchDeleteResult;
import com.zynboot.infra.storage.model.FileUploadRequest;
import com.zynboot.infra.storage.model.StoredObject;
import com.zynboot.infra.storage.model.StoredObjectMetadata;
import com.zynboot.infra.storage.model.UploadedFileInfo;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;

public interface StorageService {

    UploadedFileInfo upload(FileUploadRequest request) throws IOException;

    UploadedFileInfo upload(String key, FileUploadRequest request) throws IOException;

    UploadedFileInfo uploadToDir(String dir, FileUploadRequest request) throws IOException;

    default UploadedFileInfo upload(MultipartFile file) throws IOException {
        Assert.notNull(file, "file must not be null");
        return upload(FileUploadRequest.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .inputStreamSource(file::getInputStream)
                .build());
    }

    default UploadedFileInfo uploadToDir(String dir, MultipartFile file) throws IOException {
        Assert.notNull(file, "file must not be null");
        return uploadToDir(dir, FileUploadRequest.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .inputStreamSource(file::getInputStream)
                .build());
    }

    default UploadedFileInfo upload(String key, MultipartFile file) throws IOException {
        Assert.notNull(file, "file must not be null");
        return upload(key, FileUploadRequest.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .inputStreamSource(file::getInputStream)
                .build());
    }

    StoredObject open(String key) throws IOException;

    StoredObjectMetadata getMetadata(String key) throws IOException;

    default InputStream openStream(String key) throws IOException {
        return open(key).inputStream();
    }

    byte[] readBytes(String key) throws IOException;

    long transferTo(String key, OutputStream outputStream) throws IOException;

    Path downloadTo(String key, Path targetPath) throws IOException;

    boolean exists(String key) throws IOException;

    String getAccessUrl(String key);

    void delete(String key) throws IOException;

    BatchDeleteResult deleteBatch(Collection<String> keys);

    String copy(String sourceKey, String targetKey) throws IOException;

    String move(String sourceKey, String targetKey) throws IOException;
}
