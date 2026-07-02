package com.zynboot.infra.satoken.dao;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.util.SaFoxUtil;
import com.zynboot.infra.redis.RedisClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token Redis 持久层实现
 * <p>
 * 基于 infra 模块的 RedisClient 实现，统一 Redis 操作入口
 *
 * @author lichunqing
 */
public class RedisSaTokenDao implements SaTokenDao {

    private final RedisClient redisTemplate;

    public RedisSaTokenDao(RedisClient redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== String 操作 ====================

    @Override
    public String get(String key) {
        return redisTemplate.get(key).orElse(null);
    }

    @Override
    public void set(String key, String value, long timeout) {
        if (isInvalidTimeout(timeout)) {
            return;
        }
        if (timeout == NEVER_EXPIRE) {
            redisTemplate.put(key, value);
        } else {
            redisTemplate.put(key, value, Duration.ofSeconds(timeout));
        }
    }

    @Override
    public void update(String key, String value) {
        long expire = getTimeout(key);
        if (expire == NOT_VALUE_EXPIRE) {
            return;
        }
        set(key, value, expire);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public long getTimeout(String key) {
        return redisTemplate.ttl(key)
                .map(Duration::getSeconds)
                .orElse(NOT_VALUE_EXPIRE);
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        updateExpire(key, timeout);
    }

    // ==================== Object 操作 ====================

    @Override
    public Object getObject(String key) {
        return redisTemplate.getObject(key);
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        if (isInvalidTimeout(timeout)) {
            return;
        }
        if (timeout == NEVER_EXPIRE) {
            redisTemplate.putObject(key, object);
        } else {
            redisTemplate.putObject(key, object, Duration.ofSeconds(timeout));
        }
    }

    @Override
    public void updateObject(String key, Object object) {
        long expire = getObjectTimeout(key);
        if (expire == NOT_VALUE_EXPIRE) {
            return;
        }
        setObject(key, object, expire);
    }

    @Override
    public void deleteObject(String key) {
        redisTemplate.deleteObject(key);
    }

    @Override
    public long getObjectTimeout(String key) {
        Long expire = redisTemplate.getObjectExpire(key);
        return expire == null ? NOT_VALUE_EXPIRE : expire;
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        updateExpire(key, timeout);
    }

    // ==================== 搜索操作 ====================

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        Set<String> keys = redisTemplate.scan(prefix + "*" + keyword + "*");
        if (keys.isEmpty()) {
            return new ArrayList<>();
        }
        return SaFoxUtil.searchList(new ArrayList<>(keys), start, size, sortType);
    }

    // ==================== 私有方法 ====================

    private boolean isInvalidTimeout(long timeout) {
        return timeout == 0 || timeout <= NOT_VALUE_EXPIRE;
    }

    private void updateExpire(String key, long timeout) {
        if (timeout == NEVER_EXPIRE) {
            // 如果已永久，不做处理；否则重新设置永久
            redisTemplate.expire(key, Duration.ofSeconds(-1));
        } else {
            redisTemplate.expire(key, Duration.ofSeconds(timeout));
        }
    }
}
