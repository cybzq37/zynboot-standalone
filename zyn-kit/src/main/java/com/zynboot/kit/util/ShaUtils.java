package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * SHA 摘要工具类（SHA-1 / SHA-256 / SHA-512）。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShaUtils {

    // ==================== SHA-1 ====================

    public static String sha1(String input) {
        return HexUtils.encode(Md5Utils.digest("SHA-1", input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String sha1(byte[] data) {
        return HexUtils.encode(Md5Utils.digest("SHA-1", data));
    }

    // ==================== SHA-256 ====================

    public static String sha256(String input) {
        return HexUtils.encode(Md5Utils.digest("SHA-256", input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String sha256(byte[] data) {
        return HexUtils.encode(Md5Utils.digest("SHA-256", data));
    }

    /**
     * SHA-256 返回 byte[]（供 AES 密钥派生等使用）。
     */
    public static byte[] sha256Bytes(byte[] data) {
        return Md5Utils.digest("SHA-256", data);
    }

    public static String sha256File(java.nio.file.Path path) throws java.io.IOException {
        try (InputStream in = java.nio.file.Files.newInputStream(path)) {
            return HexUtils.encode(Md5Utils.digestStream("SHA-256", in));
        }
    }

    // ==================== SHA-512 ====================

    public static String sha512(String input) {
        return HexUtils.encode(Md5Utils.digest("SHA-512", input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String sha512(byte[] data) {
        return HexUtils.encode(Md5Utils.digest("SHA-512", data));
    }

    // ==================== HMAC-SHA256 ====================

    /**
     * HMAC-SHA256 签名（Base64 输出）。
     */
    public static String hmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    /**
     * HMAC-SHA256 签名（Hex 输出）。
     */
    public static String hmacSha256Hex(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexUtils.encode(result);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }
}
