package com.zynboot.infra.redis;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class RedisClient {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class
    );

    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private org.springframework.data.redis.core.RedisTemplate<String, Object> objectRedisTemplate;

    @Autowired(required = false)
    public void setObjectRedisTemplate(org.springframework.data.redis.core.RedisTemplate<String, Object> objectRedisTemplate) {
        this.objectRedisTemplate = objectRedisTemplate;
    }

    private StringRedisTemplate template() {
        if (redisTemplate == null) {
            throw new IllegalStateException("Redis not configured, StringRedisTemplate not available");
        }
        return redisTemplate;
    }

    public void put(String key, String value) {
        template().opsForValue().set(key, value);
    }

    public void put(String key, String value, Duration ttl) {
        template().opsForValue().set(key, value, ttl);
    }

    public void putAll(Map<String, String> values) {
        template().opsForValue().multiSet(values);
    }

    public Boolean putIfAbsent(String key, String value) {
        return template().opsForValue().setIfAbsent(key, value);
    }

    public Boolean putIfAbsent(String key, String value, Duration ttl) {
        return template().opsForValue().setIfAbsent(key, value, ttl);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(template().opsForValue().get(key));
    }

    public Optional<String> getAndDelete(String key) {
        return Optional.ofNullable(template().opsForValue().getAndDelete(key));
    }

    public List<String> multiGet(Collection<String> keys) {
        return template().opsForValue().multiGet(keys);
    }

    public Optional<String> tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean locked = putIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(locked) ? Optional.of(token) : Optional.empty();
    }

    public Boolean tryLock(String key, String token, Duration ttl) {
        return putIfAbsent(key, token, ttl);
    }

    public Boolean unlock(String key, String token) {
        Long deleted = template().execute(UNLOCK_SCRIPT, List.of(key), token);
        return deleted != null && deleted > 0;
    }

    public List<Object> executePipelined(SessionCallback<?> sessionCallback) {
        return template().executePipelined(sessionCallback);
    }

    public List<Object> executePipelined(RedisCallback<?> redisCallback) {
        return template().executePipelined(redisCallback);
    }

    public Set<String> scan(String pattern) {
        return scan(ScanOptions.scanOptions().match(pattern).count(1000).build());
    }

    public Set<String> scan(String pattern, long count) {
        return scan(ScanOptions.scanOptions().match(pattern).count(count).build());
    }

    public Set<String> scan(ScanOptions scanOptions) {
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<String> cursor = template().scan(scanOptions)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan redis keys", e);
        }
        return keys;
    }

    public Boolean exists(String key) {
        return template().hasKey(key);
    }

    public Boolean expire(String key, Duration ttl) {
        return template().expire(key, ttl);
    }

    public Optional<Duration> ttl(String key) {
        return Optional.ofNullable(template().getExpire(key))
                .filter(seconds -> seconds >= 0)
                .map(Duration::ofSeconds);
    }

    public Long increment(String key) {
        return template().opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return template().opsForValue().increment(key, delta);
    }

    public Double increment(String key, double delta) {
        return template().opsForValue().increment(key, delta);
    }

    public Long decrement(String key) {
        return template().opsForValue().decrement(key);
    }

    public Long decrement(String key, long delta) {
        return template().opsForValue().decrement(key, delta);
    }

    private org.springframework.data.redis.core.RedisTemplate<String, Object> requireObjectTemplate() {
        if (objectRedisTemplate == null) {
            throw new IllegalStateException("ObjectRedisTemplate is not available");
        }
        return objectRedisTemplate;
    }

    public boolean hasObjectTemplate() {
        return objectRedisTemplate != null;
    }

    // ==================== Object 操作 ====================

    public void putObject(String key, Object value) {
        requireObjectTemplate().opsForValue().set(key, value);
    }

    public void putObject(String key, Object value, Duration ttl) {
        requireObjectTemplate().opsForValue().set(key, value, ttl);
    }

    public Object getObject(String key) {
        return requireObjectTemplate().opsForValue().get(key);
    }

    public void deleteObject(String key) {
        requireObjectTemplate().delete(key);
    }

    public Long getObjectExpire(String key) {
        return requireObjectTemplate().getExpire(key);
    }

    public Boolean objectExpire(String key, Duration ttl) {
        return requireObjectTemplate().expire(key, ttl);
    }

    public void hashPut(String key, String hashKey, String value) {
        template().opsForHash().put(key, hashKey, value);
    }

    public void hashPutAll(String key, Map<String, String> values) {
        template().opsForHash().putAll(key, values);
    }

    public Optional<Object> hashGet(String key, String hashKey) {
        return Optional.ofNullable(template().opsForHash().get(key, hashKey));
    }

    public Map<Object, Object> hashEntries(String key) {
        return template().opsForHash().entries(key);
    }

    public Boolean hashHasKey(String key, String hashKey) {
        return template().opsForHash().hasKey(key, hashKey);
    }

    public Long hashDelete(String key, String... hashKeys) {
        return template().opsForHash().delete(key, (Object[]) hashKeys);
    }

    public Long leftPush(String key, String value) {
        return template().opsForList().leftPush(key, value);
    }

    public Long rightPush(String key, String value) {
        return template().opsForList().rightPush(key, value);
    }

    public List<String> listRange(String key, long start, long end) {
        return template().opsForList().range(key, start, end);
    }

    public Optional<String> leftPop(String key) {
        return Optional.ofNullable(template().opsForList().leftPop(key));
    }

    public Optional<String> rightPop(String key) {
        return Optional.ofNullable(template().opsForList().rightPop(key));
    }

    public Long addToSet(String key, String... values) {
        return template().opsForSet().add(key, values);
    }

    public Set<String> members(String key) {
        return template().opsForSet().members(key);
    }

    public Boolean isMember(String key, String value) {
        return template().opsForSet().isMember(key, value);
    }

    public Long removeFromSet(String key, String... values) {
        return template().opsForSet().remove(key, (Object[]) values);
    }

    public Boolean addToZSet(String key, String value, double score) {
        return template().opsForZSet().add(key, value, score);
    }

    public Set<String> zSetRange(String key, long start, long end) {
        return template().opsForZSet().range(key, start, end);
    }

    public Set<String> zSetRangeByScore(String key, double min, double max) {
        return template().opsForZSet().rangeByScore(key, min, max);
    }

    public Optional<Double> zSetScore(String key, String value) {
        return Optional.ofNullable(template().opsForZSet().score(key, value));
    }

    public Long removeFromZSet(String key, String... values) {
        return template().opsForZSet().remove(key, (Object[]) values);
    }

    public Boolean delete(String key) {
        return template().delete(key);
    }

    public Long delete(Collection<String> keys) {
        return template().delete(keys);
    }
}
