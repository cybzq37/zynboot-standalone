package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 / Base62 编解码工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Base64Utils {

    // ==================== 标准 Base64 ====================

    /**
     * 标准 Base64 编码。
     */
    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 标准 Base64 编码（String → UTF-8 bytes → Base64）。
     */
    public static String encode(String text) {
        if (StringUtils.isBlank(text)) return "";
        return encode(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 标准 Base64 解码为 byte[]。
     */
    public static byte[] decodeToBytes(String base64) {
        if (StringUtils.isBlank(base64)) return new byte[0];
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 标准 Base64 解码为 String（UTF-8）。
     */
    public static String decode(String base64) {
        return new String(decodeToBytes(base64), StandardCharsets.UTF_8);
    }

    // ==================== URL-safe Base64 ====================

    /**
     * URL-safe Base64 编码（不含 + / =，适合 URL 参数和文件名）。
     */
    public static String encodeUrlSafe(byte[] data) {
        if (data == null || data.length == 0) return "";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static String encodeUrlSafe(String text) {
        if (StringUtils.isBlank(text)) return "";
        return encodeUrlSafe(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL-safe Base64 解码为 byte[]。
     */
    public static byte[] decodeUrlSafeToBytes(String base64) {
        if (StringUtils.isBlank(base64)) return new byte[0];
        return Base64.getUrlDecoder().decode(base64);
    }

    /**
     * URL-safe Base64 解码为 String。
     */
    public static String decodeUrlSafe(String base64) {
        return new String(decodeUrlSafeToBytes(base64), StandardCharsets.UTF_8);
    }

    // ==================== MIME Base64 ====================

    /**
     * MIME Base64 编码（每 76 字符换行，适合邮件附件）。
     */
    public static String encodeMime(byte[] data) {
        if (data == null || data.length == 0) return "";
        return Base64.getMimeEncoder().encodeToString(data);
    }

    // ==================== Base62（短链/邀请码） ====================

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * long → Base62 字符串（适合短链接、邀请码）。
     * <p>
     * {@code encodeBase62(123456789L)} → {@code "8m0Kx"}
     */
    public static String encodeBase62(long value) {
        if (value == 0) return "0";
        boolean negative = value < 0;
        value = Math.abs(value);
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62_CHARS.charAt((int) (value % 62)));
            value /= 62;
        }
        if (negative) sb.append('-');
        return sb.reverse().toString();
    }

    /**
     * Base62 字符串 → long。
     */
    public static long decodeBase62(String base62) {
        if (StringUtils.isBlank(base62)) return 0;
        boolean negative = base62.startsWith("-");
        String str = negative ? base62.substring(1) : base62;
        long value = 0;
        for (char c : str.toCharArray()) {
            value = value * 62 + BASE62_CHARS.indexOf(c);
        }
        return negative ? -value : value;
    }
}
