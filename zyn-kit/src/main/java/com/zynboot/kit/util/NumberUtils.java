package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数字工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NumberUtils {

    /**
     * 安全转 int，失败返回默认值。
     */
    public static int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全转 long，失败返回默认值。
     */
    public static long toLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全转 double，失败返回默认值。
     */
    public static double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全转 BigDecimal，失败返回 null。
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 范围判断 ====================

    public static boolean between(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean between(long value, long min, long max) {
        return value >= min && value <= max;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== 格式化 ====================

    /**
     * 保留指定小数位（四舍五入）。
     */
    public static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 格式化为百分比字符串。
     *
     * {@code percent(0.856, 1)} → {@code "85.6%"}
     */
    public static String percent(double value, int scale) {
        return BigDecimal.valueOf(value * 100)
                .setScale(scale, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /**
     * 千分位格式化。
     *
     * {@code formatWithComma(1234567)} → {@code "1,234,567"}
     */
    public static String formatWithComma(long value) {
        return String.format("%,d", value);
    }

    /**
     * 数字单位格式化（万/亿）。
     *
     * {@code formatChinese(12345678)} → {@code "1234.57万"}
     */
    public static String formatChinese(long value) {
        if (value >= 100_000_000L) {
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100_000_000), 2, RoundingMode.HALF_UP)
                    .toPlainString() + "亿";
        }
        if (value >= 10_000L) {
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP)
                    .toPlainString() + "万";
        }
        return String.valueOf(value);
    }

    // ==================== 随机数 ====================

    /**
     * 生成 [min, max] 之间的随机整数。
     */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 生成 [min, max) 之间的随机 long。
     */
    public static long randomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    // ==================== 判断 ====================

    public static boolean isNumber(String str) {
        if (StringUtils.isBlank(str)) return false;
        try {
            new BigDecimal(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isInteger(String str) {
        if (StringUtils.isBlank(str)) return false;
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断是否为偶数。
     */
    public static boolean isEven(long value) {
        return (value & 1) == 0;
    }

    /**
     * 判断是否为奇数。
     */
    public static boolean isOdd(long value) {
        return (value & 1) == 1;
    }
}
