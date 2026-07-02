package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 十六进制编解码工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HexUtils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * byte[] 转十六进制字符串（小写）。
     */
    public static String encode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

    /**
     * 十六进制字符串转 byte[]。
     */
    public static byte[] decode(String hex) {
        if (StringUtils.isBlank(hex)) return new byte[0];
        hex = hex.replaceAll("\\s", "");
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high == -1 || low == -1) throw new IllegalArgumentException("Invalid hex string: " + hex);
            bytes[i] = (byte) ((high << 4) | low);
        }
        return bytes;
    }

    /**
     * 十六进制字符串转大写。
     */
    public static String toUpperCase(String hex) {
        return hex == null ? null : hex.toUpperCase();
    }

    /**
     * 判断是否为合法十六进制字符串。
     */
    public static boolean isHex(String str) {
        if (StringUtils.isBlank(str)) return false;
        return str.matches("^[0-9a-fA-F]+$");
    }

    /**
     * long 转 16 位十六进制字符串（高位补零）。
     */
    public static String fromLong(long value) {
        return String.format("%016x", value);
    }

    /**
     * 十六进制字符串转 long。
     */
    public static long toLong(String hex) {
        return Long.parseLong(hex, 16);
    }

    /**
     * int 转 8 位十六进制字符串（高位补零）。
     */
    public static String fromInt(int value) {
        return String.format("%08x", value);
    }

    /**
     * 十六进制字符串转 int。
     */
    public static int toInt(String hex) {
        return Integer.parseInt(hex, 16);
    }
}
