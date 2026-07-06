package com.zynboot.jitu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.jitu.command.geo.GeoResolveCmd;
import com.zynboot.jitu.config.JituGeoProperties;
import com.zynboot.jitu.infrastructure.entity.JituAoiView;
import com.zynboot.jitu.infrastructure.entity.JituFenceHit;
import com.zynboot.jitu.infrastructure.mapper.JituAoiMapper;
import com.zynboot.jitu.infrastructure.mapper.JituFenceMapper;
import com.zynboot.jitu.response.geo.GeoResolveRes;
import com.zynboot.kit.exception.BizException;
import com.zynboot.infra.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeoResolveService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final GeocodeService geocodeService;
    private final JituFenceMapper jituFenceMapper;
    private final JituAoiMapper jituAoiMapper;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final JituGeoProperties properties;

    public GeoResolveRes resolve(GeoResolveCmd cmd) {
        String normalizedAddress = cmd.getAddress().trim();
        String cacheKey = "jitu:geo:resolve:" + cmd.getType() + ":" +
                DigestUtils.md5DigestAsHex(normalizedAddress.getBytes(StandardCharsets.UTF_8));
        GeoResolveRes cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        GeocodeCacheValue geocode = geocodeService.geocode(normalizedAddress);
        String layerId = resolveLayerId(cmd.getType());

        JituFenceHit fenceHit = jituFenceMapper.findFirstByPoint(layerId, geocode.getLng(), geocode.getLat());
        if (fenceHit == null) {
            throw BizException.notFound("围栏");
        }

        Map<String, Object> fenceProperties = readMap(fenceHit.getProperties());
        String aoiId = readStringProperty(fenceProperties, properties.getAoiIdProperty());
        if (!StringUtils.hasText(aoiId)) {
            throw BizException.badRequest("围栏未绑定 AOI");
        }

        JituAoiView aoi = jituAoiMapper.selectEnabledById(aoiId);
        if (aoi == null) {
            throw BizException.notFound("AOI");
        }

        GeoResolveRes result = new GeoResolveRes();
        result.setLnglat(formatLngLat(fenceHit.getCenterLng(), fenceHit.getCenterLat()));
        result.setCode(readStringProperty(fenceProperties, properties.getCodeProperty()));
        result.setAddress(geocode.getFormattedAddress());
        result.setAoi(readJsonObject(aoi.getGeojson()));
        result.setFenceInfo(buildFenceInfo(fenceHit, aoiId, fenceProperties));

        writeCache(cacheKey, result);
        return result;
    }

    private String resolveLayerId(Integer type) {
        String layerId = properties.getLayerMappings().get(String.valueOf(type));
        if (!StringUtils.hasText(layerId)) {
            throw BizException.badRequest("未配置 type=" + type + " 对应的图层 ID");
        }
        return layerId;
    }

    private Map<String, Object> buildFenceInfo(JituFenceHit fenceHit,
                                               String aoiId,
                                               Map<String, Object> fenceProperties) {
        Map<String, Object> fenceInfo = new LinkedHashMap<>();
        fenceInfo.put("featureId", fenceHit.getId());
        fenceInfo.put("geojson", readJsonObject(fenceHit.getGeometry()));
        fenceInfo.put("aoiId", aoiId);
        fenceInfo.put("properties", fenceProperties);
        return fenceInfo;
    }

    private String formatLngLat(Double lng, Double lat) {
        if (lng == null || lat == null) {
            throw BizException.badRequest("围栏缺少中心点信息");
        }
        return lng + "," + lat;
    }

    private String readStringProperty(Map<String, Object> propertiesMap, String propertyName) {
        Object value = propertiesMap.get(propertyName);
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw BizException.badRequest("围栏属性 JSON 解析失败");
        }
    }

    private Object readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw BizException.badRequest("GeoJSON 解析失败");
        }
    }

    private GeoResolveRes readCache(String key) {
        try {
            Optional<String> value = redisClient.get(key);
            if (value.isEmpty() || !StringUtils.hasText(value.get())) {
                return null;
            }
            return objectMapper.readValue(value.get(), GeoResolveRes.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeCache(String key, GeoResolveRes value) {
        try {
            redisClient.put(key, objectMapper.writeValueAsString(value), properties.getResultCacheTtl());
        } catch (Exception ignored) {
            // cache is best effort
        }
    }
}
