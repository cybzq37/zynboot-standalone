package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类，用于方法入参校验。
 * <p>
 * 校验失败抛出 {@link IllegalArgumentException} 或 {@link IllegalStateException}。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AssertUtils {

    public static void isTrue(boolean expression, String message) {
        if (!expression) throw new IllegalArgumentException(message);
    }

    public static void isFalse(boolean expression, String message) {
        if (expression) throw new IllegalArgumentException(message);
    }

    public static void notNull(Object object, String message) {
        if (object == null) throw new IllegalArgumentException(message);
    }

    public static void isNull(Object object, String message) {
        if (object != null) throw new IllegalArgumentException(message);
    }

    public static void notEmpty(String str, String message) {
        if (str == null || str.isEmpty()) throw new IllegalArgumentException(message);
    }

    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) throw new IllegalArgumentException(message);
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) throw new IllegalArgumentException(message);
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) throw new IllegalArgumentException(message);
    }

    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) throw new IllegalArgumentException(message);
    }

    public static void isPositive(Number number, String message) {
        if (number == null || number.doubleValue() <= 0) throw new IllegalArgumentException(message);
    }

    public static void isNonNegative(Number number, String message) {
        if (number == null || number.doubleValue() < 0) throw new IllegalArgumentException(message);
    }

    public static void state(boolean expression, String message) {
        if (!expression) throw new IllegalStateException(message);
    }
}
