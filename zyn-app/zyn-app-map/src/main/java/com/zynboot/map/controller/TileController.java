package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.infra.redis.RedisClient;
import com.zynboot.map.response.task.TaskRes;
import com.zynboot.map.service.LayerCacheVersionService;
import com.zynboot.map.service.MapTaskService;
import com.zynboot.map.response.source.SourceTileRes;
import com.zynboot.map.service.MapTileReadService;
import com.zynboot.map.service.MapQueryFacadeService;
import com.zynboot.map.service.MvtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.zynboot.infra.web.version.ApiVersion;

import java.time.Duration;

/**
 * ZXY 瓦片服务（栅格瓦片 + 矢量瓦片 MVT）。
 * <p>
 * 栅格瓦片：/{z}/{x}/{y}.png（本地 + 代理）
 * 矢量瓦片：/{z}/{x}/{y}.mvt（PostGIS ST_AsMVT 动态生成）
 * <p>
 * 缓存策略：Redis 热瓦片 + Nginx proxy_cache 磁盘缓存。
 */
@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class TileController {

    private final RedisClient redisClient;
    private final MvtService mvtService;
    private final MapTaskService taskService;
    private final LayerCacheVersionService layerCacheVersionService;
    private final MapQueryFacadeService queryFacadeService;
    private final MapTileReadService tileReadService;

    @Value("${zyn.map.tile.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${zyn.map.tile.cache-ttl:3600}")
    private int cacheTtl;

    // ── 栅格瓦片 ───────────────────────────────────────────

    @GetMapping("/tile/{sourceId}/{z}/{x}/{y}.{format}")
    public void getTile(
            @PathVariable String sourceId,
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @PathVariable String format,
            @RequestParam(defaultValue = "3857") int srid,
            HttpServletResponse response) throws Exception {

        // MVT 走专用端点
        if ("mvt".equalsIgnoreCase(format) || "pbf".equalsIgnoreCase(format)) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"Use /mvt/ endpoint for vector tiles\"}");
            return;
        }

        // 1. 查 source 类型路由
        SourceAggregate source = queryFacadeService.requireSource(sourceId);
        long version = layerCacheVersionService.currentVersion(source.getLayerId());
        String cacheKey = "cache:tile:" + sourceId + ":v" + version + ":" + z + ":" + x + ":" + y + ":" + srid;

        // 2. 查 Redis 缓存
        if (cacheEnabled) {
            byte[] cached = getCachedTile(cacheKey);
            if (cached != null) {
                writeRasterResponse(response, getRasterContentType(format), cached, null);
                return;
            }
        }

        MapTileReadService.TilePayload tile = tileReadService.readRasterTile(source, z, x, y, format);
        if (tile == null) {
            response.setStatus(204);
            return;
        }

        // 3. 写入缓存
        if (cacheEnabled) {
            cacheTile(cacheKey, tile.getData());
        }

        // 4. 返回
        writeRasterResponse(response, tile.getContentType(), tile.getData(), null);
    }

    // ── 矢量瓦片（MVT）────────────────────────────────────

    /**
     * 矢量瓦片端点。
     * <p>
     * 路径：/mvt/{layerId}/{z}/{x}/{y}.mvt
     * 缓存：Redis 热瓦片 + Nginx 磁盘缓存（由 Cache-Control 控制）
     * 失效：图层数据变更时主动清 Redis，Nginx 通过 ETag 变化自动识别
     */
    @GetMapping("/mvt/{layerId}/{z}/{x}/{y}.pbf")
    public void getMvt(
            @PathVariable String layerId,
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @RequestParam(defaultValue = "3857") int srid,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // If-None-Match 头（浏览器/Nginx 缓存验证）
        String ifNoneMatch = request.getHeader("If-None-Match");

        MvtService.MvtResult result = mvtService.getMvt(layerId, z, x, y, srid);

        String etag = "\"" + (result.etag() != null ? result.etag() : "empty") + "\"";

        // 304 Not Modified（内容未变）
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            response.setStatus(304);
            return;
        }

        // 空瓦片
        if (result.data() == null || result.data().length == 0) {
            response.setStatus(204);
            response.setHeader("Cache-Control", "public, max-age=60");
            response.setHeader("ETag", etag);
            return;
        }

        // 写入响应
        writeMvtResponse(response, result.data(), etag);
    }

    /**
     * MVT 缓存失效端点（数据变更后调用）。
     */
    @PostMapping("/mvt/{layerId}/invalidate")
    public ApiResponse<Void> invalidateMvtCache(@PathVariable String layerId) {
        mvtService.invalidateLayerCache(layerId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/source/{id}/tile/task")
    public ApiResponse<TaskRes> submitTileTask(@PathVariable String id) {
        return ApiResponse.ok(taskService.submitTileTask(id));
    }

    // ── 瓦片状态 ───────────────────────────────────────────

    @GetMapping("/source/{id}/tile/status")
    public ApiResponse<SourceTileRes> tileStatus(@PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.getTileStatus(id));
    }

    // ── MBTiles 瓦片读取 ─────────────────────────────────────

    /**
     * 从 MBTiles 文件读取瓦片。
     * 路径：/mvt/mbtiles/{mbtilesPath}/{z}/{x}/{y}.pbf
     * mbtilesPath: MBTiles 文件路径（相对于 rasterRootPath）
     */
    @GetMapping("/mvt/mbtiles/{layerId}/{z}/{x}/{y}.pbf")
    public void getMvtFromMbtiles(
            @PathVariable String layerId,
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @RequestParam(defaultValue = "3857") int srid,
            HttpServletResponse response) throws Exception {
        byte[] tileData = tileReadService.readMbtilesTile(layerId, z, x, y);
        if (tileData == null || tileData.length == 0) {
            response.setStatus(404);
            return;
        }

        writeMvtResponse(response, tileData, "\"" + layerId + "-" + z + "-" + x + "-" + y + "\"");
    }

    // ── 响应写入 ───────────────────────────────────────────

    private void writeRasterResponse(HttpServletResponse response, String contentType, byte[] data, String etag) throws Exception {
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=300");
        if (etag != null) response.setHeader("ETag", etag);
        response.getOutputStream().write(data);
    }

    private void writeMvtResponse(HttpServletResponse response, byte[] data, String etag) throws Exception {
        response.setContentType("application/vnd.mapbox-vector-tile");
        response.setHeader("Cache-Control", "public, max-age=300");
        response.setHeader("ETag", etag);
        response.setHeader("Content-Length", String.valueOf(data.length));
        response.getOutputStream().write(data);
    }

    // ── 缓存工具 ───────────────────────────────────────────

    private byte[] getCachedTile(String key) {
        try {
            Object cached = redisClient.getObject(key);
            if (cached instanceof byte[] bytes) return bytes;
            if (cached instanceof String str) return str.getBytes();
        } catch (Exception ignored) {}
        return null;
    }

    private void cacheTile(String key, byte[] data) {
        try {
            redisClient.putObject(key, data, Duration.ofSeconds(cacheTtl));
        } catch (Exception ignored) {}
    }

    private String getRasterContentType(String format) {
        return switch (format.toLowerCase()) {
            case "jpeg", "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

}
