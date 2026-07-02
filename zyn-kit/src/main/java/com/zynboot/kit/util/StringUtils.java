package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 *
 * @author lichunqing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    public static final String SEPARATOR = ",";

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static String blankToDefault(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    public static String emptyToDefault(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 格式化文本，{@code {}} 表示占位符。
     * <p>
     * 例：{@code format("hello {}, count={}", "world", 3)} → {@code "hello world, count=3"}
     */
    public static String format(String template, Object... params) {
        if (template == null || params == null || params.length == 0) {
            return template;
        }
        String result = template;
        for (Object param : params) {
            result = result.replaceFirst("\\{\\}", java.util.regex.Matcher.quoteReplacement(String.valueOf(param)));
        }
        return result;
    }

    public static boolean isHttp(String link) {
        if (isBlank(link)) {
            return false;
        }
        try {
            URI uri = new URI(link.trim());
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static Set<String> splitToSet(String str, String separator) {
        return new HashSet<>(splitToList(str, separator, true, false));
    }

    public static List<String> splitToList(String str, String separator, boolean filterBlank, boolean trimItems) {
        List<String> list = new ArrayList<>();
        if (isEmpty(str)) {
            return list;
        }
        if (filterBlank && isBlank(str)) {
            return list;
        }
        for (String item : str.split(separator)) {
            if (filterBlank && isBlank(item)) {
                continue;
            }
            list.add(trimItems ? item.trim() : item);
        }
        return list;
    }

    public static boolean containsAnyIgnoreCase(CharSequence cs, CharSequence... searchCharSequences) {
        if (cs == null || searchCharSequences == null || searchCharSequences.length == 0) {
            return false;
        }
        for (CharSequence item : searchCharSequences) {
            if (item != null && containsIgnoreCase(cs, item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(CharSequence str, CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        int len = searchStr.length();
        int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.toString().regionMatches(true, i, searchStr.toString(), 0, len)) {
                return true;
            }
        }
        return false;
    }

    public static String toSnakeCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 8);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean inStringIgnoreCase(String str, String... strs) {
        if (strs == null || strs.length == 0) {
            return false;
        }
        for (String item : strs) {
            if (str != null && str.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    public static String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (isEmpty(camel)) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    public static String toCamelCase(String s) {
        if (isBlank(s)) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean upperNext = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                upperNext = true;
                continue;
            }
            if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static boolean matches(String str, List<String> patterns) {
        if (isEmpty(str) || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMatch(String pattern, String url) {
        if (pattern == null || url == null) return false;
        String regex = antPatternToRegex(pattern);
        return url.matches(regex);
    }

    private static String antPatternToRegex(String pattern) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                    sb.append(".*");
                    i++;
                } else {
                    sb.append("[^/]*");
                }
            } else if (c == '?') {
                sb.append('.');
            } else if (".+^${}()|[]\\".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public static String joinNotBlank(Collection<String> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        String actualSeparator = isEmpty(separator) ? SEPARATOR : separator;
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(actualSeparator));
    }

    public static String padLeft(String s, int size, char c) {
        if (s == null) {
            return String.valueOf(c).repeat(size);
        }
        if (s.length() >= size) {
            return s.substring(s.length() - size);
        }
        return String.valueOf(c).repeat(size - s.length()) + s;
    }

    public static String padLeft(Number num, int size) {
        return padLeft(String.valueOf(num), size, '0');
    }

    public static List<String> splitToList(String str) {
        return splitTo(str, String::valueOf);
    }

    public static List<String> splitToList(String str, String separator) {
        return splitTo(str, separator, String::valueOf);
    }

    public static <T> List<T> splitTo(String str, Function<? super String, T> mapper) {
        return splitTo(str, SEPARATOR, mapper);
    }

    public static <T> List<T> splitTo(String str, String separator, Function<? super String, T> mapper) {
        if (isBlank(str)) {
            return new ArrayList<>();
        }
        String actualSeparator = isEmpty(separator) ? SEPARATOR : separator;
        return Arrays.stream(str.split(java.util.regex.Pattern.quote(actualSeparator)))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(mapper)
                .collect(Collectors.toList());
    }
}
