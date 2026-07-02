package com.zynboot.kit.okhttp;

import com.zynboot.kit.util.StringUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * 表单文件数据，支持三种来源：字节数组、File、Path。
 */
public sealed interface FormPart {

    String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    String DEFAULT_FIELD_NAME = "file";

    String fieldName();
    String contentType();
    String fileName();

    // ==================== 字节数组 ====================

    record Bytes(String fieldName, String contentType, String fileName, byte[] data) implements FormPart {}

    // ==================== File ====================

    record FileRef(String fieldName, String contentType, String fileName, File file) implements FormPart {}

    // ==================== Path ====================

    record PathRef(String fieldName, String contentType, String fileName, Path path) implements FormPart {}

    // ==================== 工厂方法 ====================

    static Bytes of(String fieldName, String fileName, byte[] data) {
        return new Bytes(
                defaultFieldName(fieldName),
                DEFAULT_CONTENT_TYPE,
                defaultFileName(fileName, null),
                data
        );
    }

    static Bytes of(String fieldName, String fileName, String contentType, byte[] data) {
        return new Bytes(
                defaultFieldName(fieldName),
                defaultContentType(contentType),
                defaultFileName(fileName, null),
                data
        );
    }

    static FileRef of(String fieldName, File file) {
        return new FileRef(
                defaultFieldName(fieldName),
                DEFAULT_CONTENT_TYPE,
                defaultFileName(null, file),
                file
        );
    }

    static FileRef of(String fieldName, String contentType, File file) {
        return new FileRef(
                defaultFieldName(fieldName),
                defaultContentType(contentType),
                defaultFileName(null, file),
                file
        );
    }

    static PathRef of(String fieldName, Path path) {
        return new PathRef(
                defaultFieldName(fieldName),
                DEFAULT_CONTENT_TYPE,
                defaultFileName(null, path),
                path
        );
    }

    static PathRef of(String fieldName, String contentType, Path path) {
        return new PathRef(
                defaultFieldName(fieldName),
                defaultContentType(contentType),
                defaultFileName(null, path),
                path
        );
    }

    // ==================== 内部 ====================

    private static String defaultFieldName(String name) {
        return StringUtils.isBlank(name) ? DEFAULT_FIELD_NAME : name;
    }

    private static String defaultContentType(String ct) {
        return StringUtils.isBlank(ct) ? DEFAULT_CONTENT_TYPE : ct;
    }

    private static String defaultFileName(String fileName, Object source) {
        if (!StringUtils.isBlank(fileName)) return fileName;
        if (source instanceof File f) return f.getName();
        if (source instanceof Path p) return p.getFileName().toString();
        return DEFAULT_FIELD_NAME;
    }
}
