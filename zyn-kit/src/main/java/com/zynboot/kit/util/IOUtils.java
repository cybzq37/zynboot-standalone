package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * IO 流工具类。
 * <p>
 * 纯 Stream 操作：读取、拷贝、转换、关闭。
 * 文件操作请使用 {@link FileUtils}。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IOUtils {

    private static final int BUFFER_SIZE = 8192;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    // ==================== 安全关闭 ====================

    /**
     * 安全关闭一个或多个 Closeable，忽略异常。
     */
    public static void closeQuietly(Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable c : closeables) {
            if (c != null) {
                try { c.close(); } catch (IOException ignored) {}
            }
        }
    }

    // ==================== Stream → byte[] / String ====================

    /**
     * InputStream 读取全部内容为 byte[]。
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        if (in == null) return new byte[0];
        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            copy(in, out);
            return out.toByteArray();
        }
    }

    /**
     * InputStream 读取全部内容为 String（UTF-8）。
     */
    public static String toString(InputStream in) throws IOException {
        return toString(in, DEFAULT_CHARSET);
    }

    /**
     * InputStream 读取全部内容为 String。
     */
    public static String toString(InputStream in, Charset charset) throws IOException {
        if (in == null) return "";
        try (in; Reader reader = new InputStreamReader(in, charset)) {
            return readToString(reader);
        }
    }

    /**
     * Reader 读取全部内容为 String。
     */
    public static String toString(Reader reader) throws IOException {
        if (reader == null) return "";
        try (reader) {
            return readToString(reader);
        }
    }

    /**
     * InputStream 按行读取（UTF-8）。
     */
    public static List<String> readLines(InputStream in) throws IOException {
        return readLines(in, DEFAULT_CHARSET);
    }

    /**
     * InputStream 按行读取。
     */
    public static List<String> readLines(InputStream in, Charset charset) throws IOException {
        if (in == null) return List.of();
        try (in; BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

    // ==================== byte[] / String → Stream ====================

    /**
     * byte[] 转 InputStream。
     */
    public static InputStream toInputStream(byte[] data) {
        return data == null ? InputStream.nullInputStream() : new ByteArrayInputStream(data);
    }

    /**
     * String 转 InputStream（UTF-8）。
     */
    public static InputStream toInputStream(String text) {
        return toInputStream(text, DEFAULT_CHARSET);
    }

    /**
     * String 转 InputStream。
     */
    public static InputStream toInputStream(String text, Charset charset) {
        if (text == null) return InputStream.nullInputStream();
        return new ByteArrayInputStream(text.getBytes(charset));
    }

    // ==================== Stream 拷贝 ====================

    /**
     * InputStream → OutputStream（带缓冲）。
     */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, null);
    }

    /**
     * InputStream → OutputStream（带缓冲 + 进度回调）。
     */
    public static long copy(InputStream in, OutputStream out, LongConsumer progress) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            total += n;
            if (progress != null) progress.accept(total);
        }
        out.flush();
        return total;
    }

    /**
     * Reader → Writer（带缓冲）。
     */
    public static long copy(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[BUFFER_SIZE];
        long total = 0;
        int n;
        while ((n = reader.read(buf)) != -1) {
            writer.write(buf, 0, n);
            total += n;
        }
        writer.flush();
        return total;
    }

    // ==================== Stream ↔ File ====================

    /**
     * InputStream 写入文件（覆盖，带缓冲）。
     */
    public static long toFile(InputStream in, Path target) throws IOException {
        return toFile(in, target, null);
    }

    /**
     * InputStream 写入文件（覆盖 + 进度回调）。
     */
    public static long toFile(InputStream in, Path target, LongConsumer progress) throws IOException {
        FileUtils.ensureParentDir(target);
        try (in; OutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
            return copy(in, out, progress);
        }
    }

    /**
     * 文件转 InputStream（带缓冲）。
     */
    public static InputStream fromFile(Path path) throws IOException {
        return new BufferedInputStream(Files.newInputStream(path));
    }

    // ==================== 内部 ====================

    private static String readToString(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        char[] buf = new char[BUFFER_SIZE];
        int n;
        while ((n = reader.read(buf)) != -1) {
            writer.write(buf, 0, n);
        }
        return writer.toString();
    }
}
