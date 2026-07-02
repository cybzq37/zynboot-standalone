package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 占位符解析工具类。
 * <p>
 * 支持 {@code ${key}} 和 {@code ${key:default}} 语法。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaceholderUtils {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 解析占位符，从 Map 中取值。
     * <p>
     * {@code resolve("hello ${name}", Map.of("name", "world"))} → {@code "hello world"}
     */
    public static String resolve(String template, Map<String, String> values) {
        return resolve(template, key -> values.get(key));
    }

    /**
     * 解析占位符，支持 ${key:default} 语法。
     * <p>
     * {@code resolve("hello ${name:world}", Map.of())} → {@code "hello world"}
     */
    public static String resolveWithDefaults(String template, Map<String, String> values) {
        if (StringUtils.isBlank(template)) return template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1);
            String key, defaultValue;
            int colonIdx = expression.indexOf(':');
            if (colonIdx >= 0) {
                key = expression.substring(0, colonIdx);
                defaultValue = expression.substring(colonIdx + 1);
            } else {
                key = expression;
                defaultValue = null;
            }
            String value = values.get(key);
            if (value == null) value = defaultValue;
            if (value == null) value = matcher.group(); // 保留原始占位符
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 解析占位符，使用自定义解析函数。
     */
    public static String resolve(String template, Function<String, String> resolver) {
        if (StringUtils.isBlank(template)) return template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = resolver.apply(key);
            if (value == null) value = matcher.group(); // 保留原始占位符
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 提取模板中的所有占位符名称。
     */
    public static java.util.List<String> extractPlaceholders(String template) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (StringUtils.isBlank(template)) return keys;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String expression = matcher.group(1);
            int colonIdx = expression.indexOf(':');
            keys.add(colonIdx >= 0 ? expression.substring(0, colonIdx) : expression);
        }
        return keys;
    }

    /**
     * 判断是否包含占位符。
     */
    public static boolean hasPlaceholders(String template) {
        return !StringUtils.isBlank(template) && PLACEHOLDER_PATTERN.matcher(template).find();
    }
}
