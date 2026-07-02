package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.map.infrastructure.entity.MapAsyncTask;
import com.zynboot.map.infrastructure.entity.MapSourceTile;
import com.zynboot.map.infrastructure.mapper.MapAsyncTaskMapper;
import com.zynboot.map.infrastructure.mapper.MapSourceTileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 瓦片切片服务（异步执行）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TileService {

    private final MapSourceTileMapper tileMapper;
    private final MapAsyncTaskMapper taskMapper;
    private final LayerCacheVersionService layerCacheVersionService;

    @Value("${zyn.map.raster.root-path:./map-data/raster}")
    private String rasterRootPath;

    @Value("${zyn.map.raster.gdal-path:/usr}")
    private String gdalPath;

    @Value("${zyn.map.raster.tile-min-zoom:8}")
    private int defaultMinZoom;

    @Value("${zyn.map.raster.tile-max-zoom:18}")
    private int defaultMaxZoom;

    public MapSourceTile registerPreTiled(String sourceId, String layerId, Path tilesDir) throws Exception {
        int minZoom = Integer.MAX_VALUE;
        int maxZoom = Integer.MIN_VALUE;
        int tileCount = 0;

        if (Files.isDirectory(tilesDir)) {
            for (Path zDir : Files.list(tilesDir).toList()) {
                if (!Files.isDirectory(zDir)) continue;
                try {
                    int z = Integer.parseInt(zDir.getFileName().toString());
                    minZoom = Math.min(minZoom, z);
                    maxZoom = Math.max(maxZoom, z);
                    for (Path xDir : Files.list(zDir).toList()) {
                        if (!Files.isDirectory(xDir)) continue;
                        tileCount += (int) Files.list(xDir)
                                .filter(f -> f.toString().endsWith(".png") || f.toString().endsWith(".jpeg"))
                                .count();
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        MapSourceTile tile = new MapSourceTile();
        tile.setSourceId(sourceId);
        tile.setStatus("COMPLETED");
        tile.setPath(layerId + "/" + sourceId + "/tiles");
        tile.setMinZoom(minZoom == Integer.MAX_VALUE ? 0 : minZoom);
        tile.setMaxZoom(maxZoom == Integer.MIN_VALUE ? 18 : maxZoom);
        tile.setFormat("png");
        tile.setTileSize(256);
        tile.setTileCount(tileCount);
        tile.setProgress(100);
        tileMapper.insert(tile);

        log.info("Pre-tiled registered: sourceId={}, zoom={}-{}, tiles={}", sourceId, tile.getMinZoom(), tile.getMaxZoom(), tileCount);
        return tile;
    }

    @Async("mapTileExecutor")
    public void tileAsync(String taskId, String sourceId, String storageKey, String layerId) {
        MapAsyncTask task = taskMapper.selectById(taskId);
        if (task == null || "CANCELLED".equals(task.getStatus())) {
            return;
        }

        MapSourceTile tile = tileMapper.selectOne(
                new LambdaQueryWrapper<MapSourceTile>().eq(MapSourceTile::getSourceId, sourceId));
        if (tile == null) {
            tile = new MapSourceTile();
            tile.setSourceId(sourceId);
            tile.setFormat("png");
            tile.setTileSize(256);
            tileMapper.insert(tile);
        }

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setProgress(0);
        taskMapper.updateById(task);

        tile.setStatus("TILING");
        tile.setProgress(0);
        tileMapper.updateById(tile);

        try {
            Path rasterPath = Paths.get(storageKey);
            Path tilesDir = Paths.get(rasterRootPath, layerId, sourceId, "tiles");
            Files.createDirectories(tilesDir);

            String gdal2tiles = gdalPath + "/bin/gdal2tiles.py";
            String gdaladdo = gdalPath + "/bin/gdaladdo";

            log.info("Generating overviews: {}", storageKey);
            ProcessBuilder overviewPb = new ProcessBuilder(
                    gdaladdo, "-r", "average", rasterPath.toAbsolutePath().toString(),
                    "2", "4", "8", "16", "32");
            overviewPb.redirectErrorStream(true);
            Process overviewProc = overviewPb.start();
            overviewProc.waitFor();

            log.info("Starting tiling: sourceId={}, input={}", sourceId, storageKey);
            ProcessBuilder tilePb = new ProcessBuilder(
                    gdal2tiles,
                    "--profile=mercator",
                    "--zoom=" + defaultMinZoom + "-" + defaultMaxZoom,
                    "--webviewer=none",
                    "--resampling=bilinear",
                    "--processes=4",
                    "--xyz",
                    rasterPath.toAbsolutePath().toString(),
                    tilesDir.toAbsolutePath().toString());
            tilePb.redirectErrorStream(true);
            Process tileProc = tilePb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(tileProc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[gdal2tiles] {}", line);
                    if (isCancelled(taskId)) {
                        tileProc.destroyForcibly();
                        tile.setStatus("FAILED");
                        tile.setProgress(0);
                        tileMapper.updateById(tile);
                        return;
                    }
                    if (line.contains("Generating tiles")) {
                        tile.setProgress(50);
                        tileMapper.updateById(tile);
                        task.setProgress(50);
                        taskMapper.updateById(task);
                    }
                }
            }

            int exitCode = tileProc.waitFor();
            if (exitCode == 0) {
                long count = Files.walk(tilesDir)
                        .filter(path -> path.toString().endsWith(".png"))
                        .count();

                tile.setStatus("COMPLETED");
                tile.setProgress(100);
                tile.setPath(layerId + "/" + sourceId + "/tiles");
                tile.setMinZoom(defaultMinZoom);
                tile.setMaxZoom(defaultMaxZoom);
                tile.setTileCount((int) count);

                task.setStatus("COMPLETED");
                task.setProgress(100);
                task.setProcessedCount(1);
                task.setFinishedAt(LocalDateTime.now());

                layerCacheVersionService.bumpVersion(layerId);
                log.info("Tiling completed: sourceId={}, tiles={}", sourceId, count);
            } else {
                tile.setStatus("FAILED");
                task.setStatus("FAILED");
                task.setErrorCount(1);
                task.setErrorMessage("切片进程退出码: " + exitCode);
                task.setFinishedAt(LocalDateTime.now());
                log.error("Tiling failed: sourceId={}, exitCode={}", sourceId, exitCode);
            }
        } catch (Exception e) {
            tile.setStatus("FAILED");
            task.setStatus("FAILED");
            task.setErrorCount(1);
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            log.error("Tiling error: sourceId={}", sourceId, e);
        }

        tileMapper.updateById(tile);
        taskMapper.updateById(task);
    }

    private boolean isCancelled(String taskId) {
        MapAsyncTask current = taskMapper.selectById(taskId);
        if (current == null) {
            return false;
        }
        if ("CANCELLED".equals(current.getStatus())) {
            current.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(current);
            return true;
        }
        return false;
    }
}
