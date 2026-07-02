package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机字符串、验证码、密码生成工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtils {

    private static final String DIGITS = "0123456789";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA_DIGITS = ALPHA + DIGITS;
    private static final String HEX = "0123456789abcdef";
    private static final String SAFE_SYMBOLS = "!@#$%^&*_+-=";
    private static final String FULL = ALPHA_DIGITS + SAFE_SYMBOLS;

    // ==================== 随机字符串 ====================

    /**
     * 随机数字字符串。
     */
    public static String randomDigits(int length) {
        return randomFrom(DIGITS, length);
    }

    /**
     * 随机字母字符串。
     */
    public static String randomAlpha(int length) {
        return randomFrom(ALPHA, length);
    }

    /**
     * 随机字母+数字字符串。
     */
    public static String randomAlphaDigits(int length) {
        return randomFrom(ALPHA_DIGITS, length);
    }

    /**
     * 随机 Hex 字符串。
     */
    public static String randomHex(int length) {
        return randomFrom(HEX, length);
    }

    // ==================== 验证码 ====================

    /**
     * 纯数字验证码（默认 6 位）。
     */
    public static String captcha() {
        return captcha(6);
    }

    public static String captcha(int length) {
        return randomDigits(length);
    }

    // ==================== 密码生成 ====================

    /**
     * 生成强密码（含大小写字母+数字+特殊字符）。
     */
    public static String randomPassword(int length) {
        if (length < 8) length = 8;
        // 确保每种字符至少出现一次
        char[] chars = new char[length];
        SecureRandom sr = new SecureRandom();
        chars[0] = DIGITS.charAt(sr.nextInt(DIGITS.length()));
        chars[1] = ALPHA.charAt(sr.nextInt(26)); // 小写
        chars[2] = ALPHA.charAt(26 + sr.nextInt(26)); // 大写
        chars[3] = SAFE_SYMBOLS.charAt(sr.nextInt(SAFE_SYMBOLS.length()));
        for (int i = 4; i < length; i++) {
            chars[i] = FULL.charAt(sr.nextInt(FULL.length()));
        }
        // 打乱顺序
        for (int i = chars.length - 1; i > 0; i--) {
            int j = sr.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    // ==================== UUID 短版 ====================

    /**
     * 32 位随机 Hex（类似 UUID 但更短）。
     */
    public static String randomId() {
        return randomHex(32);
    }

    /**
     * 16 位随机 ID。
     */
    public static String shortId() {
        return randomHex(16);
    }

    // ==================== 内部 ====================

    private static String randomFrom(String chars, int length) {
        if (length <= 0) return "";
        ThreadLocalRandom r = ThreadLocalRandom.current();
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = chars.charAt(r.nextInt(chars.length()));
        }
        return new String(buf);
    }
}
