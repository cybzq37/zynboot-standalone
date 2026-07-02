package com.zynboot.kit.util;

import java.io.Closeable;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 本地 TTL 缓存（不依赖 Redis）。
 * <p>
 * 适用于配置缓存、热点数据缓存等场景。实现 {@link Closeable} 以释放后台清理任务。
 *
 * <pre>
 * try (CacheUtils<String, String> cache = CacheUtils.of(Duration.ofMinutes(5))) {
 *     cache.get("key", k -> expensiveLookup(k));
 * }
 * </pre>
 */
public class CacheUtils<K, V> implements Closeable {

    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cache-cleaner");
                t.setDaemon(true);
                return t;
            });

    private final ConcurrentHashMap<K, Entry<V>> store = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<K> insertionOrder = new ConcurrentLinkedDeque<>();
    private final Duration ttl;
    private final int maxSize;
    private final ScheduledFuture<?> cleanupTask;

    private CacheUtils(Duration ttl, int maxSize) {
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.cleanupTask = CLEANER.scheduleWithFixedDelay(
                this::evictExpired, ttl.toSeconds(), ttl.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 创建 TTL 缓存（默认最大 1000 条）。
     */
    public static <K, V> CacheUtils<K, V> of(Duration ttl) {
        return new CacheUtils<>(ttl, 1000);
    }

    /**
     * 创建 TTL 缓存（自定义最大条数）。
     */
    public static <K, V> CacheUtils<K, V> of(Duration ttl, int maxSize) {
        return new CacheUtils<>(ttl, maxSize);
    }

    /**
     * 获取缓存值，不存在时通过 loader 加载。
     */
    public V get(K key, Function<K, V> loader) {
        Entry<V> entry = store.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        V value = loader.apply(key);
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    /**
     * 获取缓存值，不存在返回 null。
     */
    public V getIfPresent(K key) {
        Entry<V> entry = store.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        store.remove(key);
        return null;
    }

    /**
     * 写入缓存。
     */
    public synchronized void put(K key, V value) {
        if (store.size() >= maxSize) {
            evictOldest();
        }
        Entry<V> prev = store.put(key, new Entry<>(value, System.nanoTime() + ttl.toNanos()));
        if (prev == null) {
            insertionOrder.addLast(key);
        }
    }

    /**
     * 移除指定 key。
     */
    public void invalidate(K key) {
        store.remove(key);
        insertionOrder.remove(key);
    }

    /**
     * 清空全部缓存。
     */
    public void invalidateAll() {
        store.clear();
        insertionOrder.clear();
    }

    /**
     * 当前缓存条数。
     */
    public int size() {
        return store.size();
    }

    /**
     * 导出所有未过期的 key-value。
     */
    public Map<K, V> asMap() {
        Map<K, V> map = new LinkedHashMap<>();
        store.forEach((k, v) -> {
            if (!v.isExpired()) map.put(k, v.value);
        });
        return map;
    }

    /**
     * 取消后台清理任务，释放资源。
     */
    @Override
    public void close() {
        cleanupTask.cancel(false);
    }

    // ==================== 内部 ====================

    private void evictExpired() {
        store.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private void evictOldest() {
        while (!insertionOrder.isEmpty()) {
            K oldest = insertionOrder.pollFirst();
            if (oldest != null && store.remove(oldest) != null) {
                return;
            }
        }
    }

    private record Entry<V>(V value, long expireAt) {
        boolean isExpired() {
            return System.nanoTime() > expireAt;
        }
    }
}
