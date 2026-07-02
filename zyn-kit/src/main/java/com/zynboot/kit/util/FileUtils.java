package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 文件工具类。
 * <p>
 * 路径参数统一使用 {@link Path}，不暴露 {@link java.io.File}。
 * 默认 UTF-8，自动创建父目录。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    // ==================== 文件读取 ====================

    /**
     * 读取文件全部内容为 byte[]。
     */
    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * 读取文件全部内容为 String（UTF-8）。
     */
    public static String readString(Path path) throws IOException {
        return Files.readString(path, DEFAULT_CHARSET);
    }

    /**
     * 读取文件按行（UTF-8）。
     */
    public static List<String> readLines(Path path) throws IOException {
        return Files.readAllLines(path, DEFAULT_CHARSET);
    }

    // ==================== 文件写入 ====================

    /**
     * 写入 byte[] 到文件（覆盖）。
     */
    public static void writeBytes(Path path, byte[] data) throws IOException {
        ensureParentDir(path);
        Files.write(path, data);
    }

    /**
     * 写入 String 到文件（覆盖，UTF-8）。
     */
    public static void writeString(Path path, String content) throws IOException {
        ensureParentDir(path);
        Files.writeString(path, content, DEFAULT_CHARSET);
    }

    /**
     * 写入多行到文件（覆盖，UTF-8）。
     */
    public static void writeLines(Path path, List<String> lines) throws IOException {
        ensureParentDir(path);
        Files.write(path, lines, DEFAULT_CHARSET);
    }

    /**
     * 追加 String 到文件末尾（UTF-8）。
     */
    public static void appendString(Path path, String content) throws IOException {
        ensureParentDir(path);
        Files.writeString(path, content, DEFAULT_CHARSET,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 追加一行到文件末尾（UTF-8）。
     */
    public static void appendLine(Path path, String line) throws IOException {
        appendString(path, line + System.lineSeparator());
    }

    // ==================== 文件拷贝/移动/删除 ====================

    /**
     * 拷贝文件（覆盖已存在）。
     */
    public static Path copyFile(Path source, Path target) throws IOException {
        ensureParentDir(target);
        return Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 移动/重命名文件（覆盖已存在）。
     */
    public static Path moveFile(Path source, Path target) throws IOException {
        ensureParentDir(target);
        return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 拷贝目录（递归）。
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 删除文件或目录（递归），不存在时忽略。
     */
    public static void delete(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(path);
        }
    }

    // ==================== 目录操作 ====================

    /**
     * 创建目录（含父目录）。
     */
    public static void mkdirs(Path path) throws IOException {
        Files.createDirectories(path);
    }

    /**
     * 列出目录下所有文件（非递归）。
     */
    public static List<Path> listFiles(Path dir) throws IOException {
        if (!isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    /**
     * 列出目录下所有子目录（非递归）。
     */
    public static List<Path> listDirectories(Path dir) throws IOException {
        if (!isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).toList();
        }
    }

    /**
     * 递归列出目录下所有文件。
     */
    public static List<Path> walkFiles(Path dir) throws IOException {
        if (!isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    /**
     * 递归列出目录下指定扩展名的文件。
     */
    public static List<Path> walkFiles(Path dir, String extension) throws IOException {
        if (!isDirectory(dir)) return List.of();
        String ext = extension.startsWith(".") ? extension : "." + extension;
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(ext))
                    .toList();
        }
    }

    // ==================== 文件状态 ====================

    public static boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    public static boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    public static boolean isDirectory(Path path) {
        return path != null && Files.isDirectory(path);
    }

    public static boolean isEmpty(Path path) throws IOException {
        return !exists(path) || Files.size(path) == 0;
    }

    public static long size(Path path) throws IOException {
        return exists(path) ? Files.size(path) : -1;
    }

    // ==================== 文件名/路径 ====================

    /**
     * 获取文件扩展名（不含点号）。
     */
    public static String getExtension(Path path) {
        if (path == null) return "";
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    /**
     * 获取文件名（不含扩展名）。
     */
    public static String getBaseName(Path path) {
        if (path == null) return "";
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * 获取 MIME 类型（基于扩展名，Java NIO 探测 + 内置映射兜底）。
     */
    public static String getMimeType(Path path) {
        if (path == null) return "application/octet-stream";
        try {
            String probed = Files.probeContentType(path);
            if (probed != null) return probed;
        } catch (IOException ignored) {
        }
        return getMimeTypeByExtension(getExtension(path));
    }

    /**
     * 获取 MIME 类型（基于文件名）。
     */
    public static String getMimeType(String filename) {
        if (StringUtils.isBlank(filename)) return "application/octet-stream";
        int dot = filename.lastIndexOf('.');
        String ext = dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
        return getMimeTypeByExtension(ext);
    }

    /**
     * 格式化文件大小为人类可读格式。
     * <p>
     * {@code humanSize(1536000)} → {@code "1.46 MB"}
     */
    public static String humanSize(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        double size = bytes;
        for (String unit : units) {
            size /= 1024;
            if (size < 1024) return String.format("%.2f %s", size, unit);
        }
        return String.format("%.2f EB", size / 1024);
    }

    /**
     * 格式化文件大小（读取文件）。
     */
    public static String humanSize(Path path) throws IOException {
        return humanSize(size(path));
    }

    /**
     * 文件名安全化（移除文件系统非法字符）。
     */
    public static String sanitizeFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) return "unnamed";
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        sanitized = sanitized.trim().replaceAll("^[.]+|[.]+$", "");
        return StringUtils.isBlank(sanitized) ? "unnamed" : sanitized;
    }

    // ==================== 临时文件 ====================

    /**
     * 创建临时文件。
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }

    /**
     * 创建临时目录。
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    // ==================== 内部 ====================

    static void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private static final Map<String, String> MIME_MAP = Map.ofEntries(
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("zip", "application/zip"),
            Map.entry("gz", "application/gzip"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("js", "application/javascript"),
            Map.entry("ts", "application/typescript"),
            Map.entry("yaml", "text/yaml"),
            Map.entry("yml", "text/yaml"),
            Map.entry("toml", "text/toml"),
            Map.entry("sh", "application/x-sh"),
            Map.entry("sql", "application/sql"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("otf", "font/otf"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2")
    );

    private static String getMimeTypeByExtension(String ext) {
        if (StringUtils.isBlank(ext)) return "application/octet-stream";
        return MIME_MAP.getOrDefault(ext.toLowerCase(), "application/octet-stream");
    }
}
