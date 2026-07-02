package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5 摘要工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Md5Utils {

    /**
     * 字符串 MD5（小写）。
     */
    public static String md5(String input) {
        if (input == null) return null;
        return HexUtils.encode(digest("MD5", input.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * byte[] MD5（小写）。
     */
    public static String md5(byte[] data) {
        if (data == null) return null;
        return HexUtils.encode(digest("MD5", data));
    }

    /**
     * InputStream MD5（小写）。
     */
    public static String md5(InputStream in) {
        return HexUtils.encode(digestStream("MD5", in));
    }

    /**
     * 带盐 MD5。
     */
    public static String md5WithSalt(String input, String salt) {
        if (input == null) return null;
        return md5(input + salt);
    }

    /**
     * 文件 MD5。
     */
    public static String md5File(java.nio.file.Path path) throws java.io.IOException {
        try (InputStream in = java.nio.file.Files.newInputStream(path)) {
            return md5(in);
        }
    }

    // ==================== 内部 ====================

    static byte[] digest(String algorithm, byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(algorithm + " digest failed", e);
        }
    }

    static byte[] digestStream(String algorithm, InputStream in) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException(algorithm + " digest failed", e);
        }
    }
}
