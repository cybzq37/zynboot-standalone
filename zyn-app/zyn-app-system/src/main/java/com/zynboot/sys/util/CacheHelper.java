package com.zynboot.sys.util;

import com.zynboot.infra.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Component
public class CacheHelper {

    /** 空值占位符，防止缓存穿透（loader 返回 null 时短 TTL 缓存该标记）。 */
    private static final String NULL_MARKER = "__ZYN_NULL__";
    /** 空值缓存的短 TTL。 */
    private static final Duration NULL_TTL = Duration.ofSeconds(60);

    private final RedisClient redisClient;

    public CacheHelper(@Nullable RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public boolean available() {
        return redisClient != null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        if (redisClient != null) {
            try {
                Object cached = redisClient.getObject(key);
                if (NULL_MARKER.equals(cached)) {
                    return null;
                }
                if (cached != null) {
                    return (T) cached;
                }
            } catch (Exception e) {
                log.debug("Redis getObject failed, falling back to loader: {}", e.getMessage());
            }
        }
        T value = loader.get();
        if (redisClient != null) {
            try {
                if (value != null) {
                    redisClient.putObject(key, value, withJitter(ttl));
                } else {
                    // 缓存空值（短 TTL），抵御缓存穿透
                    redisClient.putObject(key, NULL_MARKER, NULL_TTL);
                }
            } catch (Exception e) {
                log.debug("Redis putObject failed: {}", e.getMessage());
            }
        }
        return value;
    }

    /**
     * 给 TTL 追加 0~10% 的随机抖动，避免大量键同一时刻集中过期（缓存雪崩）。
     */
    private static Duration withJitter(Duration ttl) {
        long seconds = ttl.toSeconds();
        if (seconds <= 0) {
            return ttl;
        }
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, seconds / 10 + 1));
        return ttl.plusSeconds(jitter);
    }

    public void evict(String... keys) {
        if (redisClient != null) {
            for (String key : keys) {
                redisClient.delete(key);
            }
        }
    }
}
