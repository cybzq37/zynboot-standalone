package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectionUtils {

    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> c) {
        return !isEmpty(c);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 安全获取 List 元素，越界返回 null。
     */
    public static <T> T get(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    /**
     * 获取 List 第一个元素。
     */
    public static <T> T getFirst(List<T> list) {
        return isEmpty(list) ? null : list.get(0);
    }

    /**
     * 获取 List 最后一个元素。
     */
    public static <T> T getLast(List<T> list) {
        return isEmpty(list) ? null : list.get(list.size() - 1);
    }

    /**
     * 按 key 去重，保留第一个。
     */
    public static <T> List<T> distinctByKey(List<T> list, Function<T, ?> keyFn) {
        if (isEmpty(list)) return Collections.emptyList();
        Set<Object> seen = new HashSet<>();
        return list.stream()
                .filter(item -> seen.add(keyFn.apply(item)))
                .collect(Collectors.toList());
    }

    /**
     * 合并多个集合。
     */
    @SafeVarargs
    public static <T> List<T> union(List<T>... lists) {
        List<T> result = new ArrayList<>();
        for (List<T> list : lists) {
            if (isNotEmpty(list)) {
                result.addAll(list);
            }
        }
        return result;
    }

    /**
     * 交集。
     */
    public static <T> List<T> intersection(List<T> a, List<T> b) {
        if (isEmpty(a) || isEmpty(b)) return Collections.emptyList();
        Set<T> setB = new HashSet<>(b);
        return a.stream().filter(setB::contains).collect(Collectors.toList());
    }

    /**
     * 差集（a 中有 b 中没有的）。
     */
    public static <T> List<T> subtract(List<T> a, List<T> b) {
        if (isEmpty(a)) return Collections.emptyList();
        if (isEmpty(b)) return new ArrayList<>(a);
        Set<T> setB = new HashSet<>(b);
        return a.stream().filter(item -> !setB.contains(item)).collect(Collectors.toList());
    }

    /**
     * 分批。
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (isEmpty(list) || size <= 0) return Collections.emptyList();
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    /**
     * List 转 Map，处理重复 key。
     */
    public static <T, K> Map<K, T> toMap(List<T> list, Function<T, K> keyFn) {
        if (isEmpty(list)) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(keyFn, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 过滤并收集。
     */
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        if (isEmpty(list)) return Collections.emptyList();
        return list.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 转换元素类型。
     */
    public static <T, R> List<R> map(List<T> list, Function<T, R> mapper) {
        if (isEmpty(list)) return Collections.emptyList();
        return list.stream().map(mapper).collect(Collectors.toList());
    }
}
