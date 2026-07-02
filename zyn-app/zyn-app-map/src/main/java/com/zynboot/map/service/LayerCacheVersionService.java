package com.zynboot.map.service;

import com.zynboot.infra.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 图层缓存版本号服务。
 * 使用版本号递增代替全量 scan 删除，避免大 keyspace 下的缓存失效风暴。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayerCacheVersionService {

    private static final Duration VERSION_TTL = Duration.ofDays(30);

    private final RedisClient redisClient;

    private final ConcurrentMap<String, AtomicLong> localVersions = new ConcurrentHashMap<>();

    public long currentVersion(String layerId) {
        String key = buildKey(layerId);
        try {
            Optional<String> value = redisClient.get(key);
            if (value.isPresent()) {
                long version = Long.parseLong(value.get());
                localVersions.computeIfAbsent(layerId, ignored -> new AtomicLong(version)).set(version);
                return version;
            }
            redisClient.putIfAbsent(key, "1", VERSION_TTL);
            return localVersions.computeIfAbsent(layerId, ignored -> new AtomicLong(1)).get();
        } catch (Exception e) {
            return localVersions.computeIfAbsent(layerId, ignored -> new AtomicLong(1)).get();
        }
    }

    public long bumpVersion(String layerId) {
        String key = buildKey(layerId);
        try {
            Long version = redisClient.increment(key);
            redisClient.expire(key, VERSION_TTL);
            long resolved = version != null && version > 0 ? version : 1L;
            localVersions.computeIfAbsent(layerId, ignored -> new AtomicLong(resolved)).set(resolved);
            return resolved;
        } catch (Exception e) {
            long version = localVersions.computeIfAbsent(layerId, ignored -> new AtomicLong(1)).incrementAndGet();
            log.debug("Layer cache version fallback to local counter: layerId={}, version={}", layerId, version);
            return version;
        }
    }

    private String buildKey(String layerId) {
        return "cache:layer:version:" + layerId;
    }
}
