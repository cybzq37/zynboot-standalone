package com.zynboot.map.service;

import com.zynboot.infra.redis.RedisClient;
import com.zynboot.map.infrastructure.mapper.MapMvtMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 矢量瓦片服务（MVT）。
 * <p>
 * 缓存策略：Redis 热瓦片 + Nginx 磁盘缓存。
 * - Redis：小容量（~50MB），缓存高频访问瓦片，TTL 5 分钟
 * - Nginx：大容量（~10GB），proxy_cache 缓存所有瓦片，由 HTTP 头控制
 * - 数据变更时主动失效 Redis 缓存，Nginx 通过 ETag 变化自动识别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MvtService {

    private final MapMvtMapper mvtMapper;
    private final RedisClient redisClient;
    private final LayerCacheVersionService layerCacheVersionService;

    @Value("${zyn.map.tile.mvt-cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${zyn.map.tile.mvt-cache-ttl:300}")
    private int cacheTtl;

    @Value("${zyn.map.tile.mvt-max-features:50000}")
    private int maxFeatures;

    /**
     * 生成 MVT 瓦片。
     * 优先查 Redis，未命中则调 PostGIS ST_AsMVT 生成。
     */
    public MvtResult getMvt(String layerId, int z, int x, int y, int srid) {
        long version = layerCacheVersionService.currentVersion(layerId);
        String cacheKey = buildCacheKey(layerId, version, z, x, y, srid);
        String etag = null;

        // 1. 计算 ETag（用于 Nginx/浏览器 304 判断）
        try {
            etag = mvtMapper.getLayerEtag(layerId);
        } catch (Exception ignored) {}

        // 2. 查 Redis 缓存
        if (cacheEnabled) {
            try {
                Object cached = redisClient.getObject(cacheKey);
                if (cached instanceof byte[] bytes && bytes.length > 0) {
                    log.debug("MVT cache hit: {}", cacheKey);
                    return new MvtResult(bytes, etag, true);
                }
            } catch (Exception e) {
                log.debug("MVT cache read error: {}", e.getMessage());
            }
        }

        // 3. 检查是否有要素（空瓦片优化）
        boolean hasFeatures;
        try {
            hasFeatures = mvtMapper.hasFeatures(layerId, z, x, y);
        } catch (Exception e) {
            hasFeatures = true; // 查询失败时保守生成
        }

        if (!hasFeatures) {
            return new MvtResult(new byte[0], etag, false);
        }

        // 4. PostGIS ST_AsMVT 生成
        byte[] mvt;
        try {
            mvt = mvtMapper.generateMvt(layerId, z, x, y, srid, maxFeatures);
        } catch (Exception e) {
            log.error("MVT generation failed: layerId={}, z={}, x={}, y={}", layerId, z, x, y, e);
            return new MvtResult(null, etag, false);
        }

        if (mvt == null || mvt.length == 0) {
            return new MvtResult(new byte[0], etag, false);
        }

        // 5. 写入 Redis 缓存
        if (cacheEnabled) {
            try {
                redisClient.putObject(cacheKey, mvt, Duration.ofSeconds(cacheTtl));
                log.debug("MVT cached: {} ({} bytes)", cacheKey, mvt.length);
            } catch (Exception e) {
                log.debug("MVT cache write error: {}", e.getMessage());
            }
        }

        return new MvtResult(mvt, etag, false);
    }

    /**
     * 失效某图层的所有 MVT 缓存。
     * 数据变更时调用（导入/编辑/删除要素后）。
     */
    public void invalidateLayerCache(String layerId) {
        long version = layerCacheVersionService.bumpVersion(layerId);
        log.info("MVT cache version bumped: layerId={}, version={}", layerId, version);
    }

    private String buildCacheKey(String layerId, long version, int z, int x, int y, int srid) {
        return String.format("cache:mvt:%s:v%d:%d:%d:%d:%d", layerId, version, z, x, y, srid);
    }

    public record MvtResult(byte[] data, String etag, boolean fromCache) {}
}
