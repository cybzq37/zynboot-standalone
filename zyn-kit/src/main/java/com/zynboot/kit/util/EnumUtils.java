package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 枚举工具类，安全转换避免 NPE。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnumUtils {

    /**
     * 按 name 安全转换，找不到返回 null。
     */
    public static <E extends Enum<E>> E ofName(Class<E> enumType, String name) {
        if (enumType == null || StringUtils.isBlank(name)) return null;
        try {
            return Enum.valueOf(enumType, name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 按 name 转换，找不到返回默认值。
     */
    public static <E extends Enum<E>> E ofName(Class<E> enumType, String name, E defaultValue) {
        E result = ofName(enumType, name);
        return result != null ? result : defaultValue;
    }

    /**
     * 按自定义 code 字段查找枚举（通过 getter 反射）。
     * <p>
     * 适用场景：枚举有 getCode() 方法，业务用 code 值而非 name 匹配。
     */
    public static <E extends Enum<E>> E ofCode(Class<E> enumType, Object code, Function<E, Object> codeGetter) {
        if (enumType == null || code == null) return null;
        for (E e : enumType.getEnumConstants()) {
            if (code.equals(codeGetter.apply(e))) return e;
        }
        return null;
    }

    /**
     * 按 code 查找，找不到返回默认值。
     */
    public static <E extends Enum<E>> E ofCode(Class<E> enumType, Object code, Function<E, Object> codeGetter, E defaultValue) {
        E result = ofCode(enumType, code, codeGetter);
        return result != null ? result : defaultValue;
    }

    /**
     * 安全转 int code（枚举 ordinal），越界返回默认值。
     */
    public static <E extends Enum<E>> E ofOrdinal(Class<E> enumType, int ordinal) {
        E[] values = enumType.getEnumConstants();
        if (values == null || ordinal < 0 || ordinal >= values.length) return null;
        return values[ordinal];
    }

    /**
     * 获取所有枚举值。
     */
    public static <E extends Enum<E>> List<E> allOf(Class<E> enumType) {
        return Arrays.asList(enumType.getEnumConstants());
    }

    /**
     * 枚举转 Map（name → enum）。
     */
    public static <E extends Enum<E>> Map<String, E> toMap(Class<E> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .collect(Collectors.toMap(Enum::name, e -> e));
    }

    /**
     * 判断 name 是否为合法枚举值。
     */
    public static <E extends Enum<E>> boolean isValid(Class<E> enumType, String name) {
        return ofName(enumType, name) != null;
    }
}
