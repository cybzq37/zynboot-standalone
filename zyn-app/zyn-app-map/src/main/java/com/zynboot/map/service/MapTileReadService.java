package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapSourceProxy;
import com.zynboot.map.infrastructure.entity.MapSourceTile;
import com.zynboot.map.infrastructure.mapper.MapSourceProxyMapper;
import com.zynboot.map.infrastructure.mapper.MapSourceTileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapTileReadService {

    private final SourceRepository sourceRepository;
    private final MapSourceTileMapper tileMapper;
    private final MapSourceProxyMapper proxyMapper;
    private final OkHttpClient httpClient;

    @Value("${zyn.map.raster.root-path:./map-data/raster}")
    private String rasterRootPath;

    public SourceAggregate requireSource(String sourceId) {
        return sourceRepository.findById(sourceId)
                .orElseThrow(() -> BizException.notFound("数据源"));
    }

    public TilePayload readRasterTile(String sourceId, int z, int x, int y, String format) {
        return readRasterTile(requireSource(sourceId), z, x, y, format);
    }

    public TilePayload readRasterTile(SourceAggregate source, int z, int x, int y, String format) {
        byte[] data = "FILE".equals(source.getType())
                ? serveLocalTile(source, z, x, y, format)
                : proxyExternalTile(source, z, x, y, format);
        if (data == null || data.length == 0) {
            return null;
        }
        return new TilePayload(source.getLayerId(), getContentType(format), data);
    }

    public byte[] readMbtilesTile(String layerId, int z, int x, int y) {
        Path mbtilesPath = Paths.get(rasterRootPath, layerId, layerId + ".mbtiles");
        if (!Files.exists(mbtilesPath)) {
            return null;
        }

        int tmsY = (1 << z) - 1 - y;
        try (var conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + mbtilesPath);
             var ps = conn.prepareStatement(
                     "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?")) {
            ps.setInt(1, z);
            ps.setInt(2, x);
            ps.setInt(3, tmsY);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("tile_data");
                }
            }
        } catch (Exception e) {
            log.error("MBTiles read error: layerId={}, z={}, x={}, y={}", layerId, z, x, y, e);
        }
        return null;
    }

    private byte[] serveLocalTile(SourceAggregate source, int z, int x, int y, String format) {
        try {
            MapSourceTile tile = tileMapper.selectOne(
                    new LambdaQueryWrapper<MapSourceTile>().eq(MapSourceTile::getSourceId, source.getId()));
            if (tile == null || !"COMPLETED".equals(tile.getStatus())) {
                return null;
            }

            String tilePath = tile.getPath() != null ? tile.getPath() : source.getId() + "/tiles";
            Path filePath = Paths.get(rasterRootPath, source.getEntity().getLayerId(), tilePath,
                    String.valueOf(z), String.valueOf(x), y + "." + format);
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (Exception e) {
            log.error("Read local tile failed: sourceId={}, z={}, x={}, y={}", source.getId(), z, x, y, e);
            return null;
        }
    }

    private byte[] proxyExternalTile(SourceAggregate source, int z, int x, int y, String format) {
        MapSourceProxy proxy = proxyMapper.selectOne(
                new LambdaQueryWrapper<MapSourceProxy>().eq(MapSourceProxy::getSourceId, source.getId()));
        if (proxy == null) {
            return null;
        }
        if ("DOWN".equals(proxy.getHealthStatus())) {
            log.warn("External service DOWN: sourceId={}", source.getId());
            return null;
        }

        String url = buildExternalUrl(proxy, source.getType(), z, x, y, format);
        if (url == null) {
            return null;
        }

        try {
            Request.Builder reqBuilder = new Request.Builder().url(url);
            if (proxy.getAuthType() != null && !"NONE".equals(proxy.getAuthType())) {
                attachAuth(reqBuilder, proxy);
            }
            try (Response resp = httpClient.newCall(reqBuilder.build()).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    return resp.body().bytes();
                }
                log.warn("Proxy tile failed: sourceId={}, status={}", source.getId(), resp.code());
                return null;
            }
        } catch (Exception e) {
            log.error("Proxy tile error: sourceId={}", source.getId(), e);
            int failCount = proxy.getFailCount() != null ? proxy.getFailCount() : 0;
            proxy.setFailCount(failCount + 1);
            proxyMapper.updateById(proxy);
            return null;
        }
    }

    private String buildExternalUrl(MapSourceProxy proxy, String sourceType, int z, int x, int y, String format) {
        String baseUrl = proxy.getUrl();
        if (baseUrl == null) {
            return null;
        }
        String base = baseUrl.replaceAll("/$", "");
        return switch (sourceType) {
            case "WMTS" -> {
                String layer = proxy.getWmtsLayer() != null ? proxy.getWmtsLayer() : "";
                String style = proxy.getWmtsStyle() != null ? proxy.getWmtsStyle() : "default";
                String matrixSet = proxy.getWmtsMatrixSet() != null ? proxy.getWmtsMatrixSet() : "default";
                String fmt = proxy.getWmtsFormat() != null ? proxy.getWmtsFormat() : "image/" + format;
                yield base + "?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile"
                        + "&LAYER=" + layer
                        + "&STYLE=" + style
                        + "&TILEMATRIXSET=" + matrixSet
                        + "&TILEMATRIX=" + z
                        + "&TILEROW=" + y
                        + "&TILECOL=" + x
                        + "&FORMAT=" + fmt;
            }
            case "WMS" -> {
                double resolution = 40075016.68557849 / (256 * Math.pow(2, z));
                double minx = x * 256 * resolution - 20037508.342789244;
                double miny = 20037508.342789244 - (y + 1) * 256 * resolution;
                double maxx = (x + 1) * 256 * resolution - 20037508.342789244;
                double maxy = 20037508.342789244 - y * 256 * resolution;
                String layers = proxy.getWmtsLayer() != null ? proxy.getWmtsLayer() : "";
                yield base + "?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap"
                        + "&LAYERS=" + layers
                        + "&BBOX=" + minx + "," + miny + "," + maxx + "," + maxy
                        + "&SRS=EPSG:3857"
                        + "&WIDTH=256&HEIGHT=256"
                        + "&FORMAT=image/" + format;
            }
            case "TMS" -> base + "/" + z + "/" + x + "/" + ((1 << z) - 1 - y) + "." + format;
            default -> base + "/" + z + "/" + x + "/" + y + "." + format;
        };
    }

    private void attachAuth(Request.Builder builder, MapSourceProxy proxy) {
        switch (proxy.getAuthType()) {
            case "BASIC" -> {
                String encoded = java.util.Base64.getEncoder()
                        .encodeToString((proxy.getAuthValue() != null ? proxy.getAuthValue() : "").getBytes());
                builder.addHeader("Authorization", "Basic " + encoded);
            }
            case "TOKEN" -> builder.addHeader("Authorization", "Bearer " + proxy.getAuthValue());
            case "API_KEY" -> {
                String header = proxy.getAuthHeader() != null ? proxy.getAuthHeader() : "X-API-Key";
                builder.addHeader(header, proxy.getAuthValue());
            }
            default -> {
            }
        }
    }

    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "jpeg", "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

    @lombok.Value
    public static class TilePayload {
        String layerId;
        String contentType;
        byte[] data;
    }
}
