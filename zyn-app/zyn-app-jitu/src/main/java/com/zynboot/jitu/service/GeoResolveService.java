package com.zynboot.jitu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.jitu.config.JituGeoProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeoResolveService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /** fenceInfo.properties 白名单：仅返回这四个业务字段 */
    private static final List<String> FENCE_PROPERTY_WHITELIST = List.of("code", "name", "type", "aoi_id");

    private final GeocodeService geocodeService;
    private final JituFenceMapper jituFenceMapper;
    private final JituAoiMapper jituAoiMapper;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final JituGeoProperties properties;

    public GeoResolveRes resolve(Integer type, String address) {
        String normalizedAddress = address.trim();
        String cacheKey = "jitu:geo:resolve:" + type + ":" +
                DigestUtils.md5DigestAsHex(normalizedAddress.getBytes(StandardCharsets.UTF_8));
        GeoResolveRes cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 1. 根据地址调用地图服务，获取经纬度坐标和标准化地址
        GeocodeCacheValue geocode = geocodeService.geocode(normalizedAddress);

        // 2. 根据经纬度坐标，从 layer-jitu 图层中获取命中的面数据（通过 properties.type 区分取件/收件）
        String layerId = resolveLayerId();
        JituFenceHit fenceHit = jituFenceMapper.findFirstByPoint(layerId, String.valueOf(type), geocode.getLng(), geocode.getLat());
        if (fenceHit == null) {
            throw BizException.notFound("围栏");
        }

        Map<String, Object> fenceProperties = readMap(fenceHit.getProperties());

        // 3. 从 layer-aoi-bp 和 layer-aoi-building 查询 AOI 几何（BP 优先，命中即返回）
        Object aoi = buildAoi(geocode.getLng(), geocode.getLat());

        GeoResolveRes result = new GeoResolveRes();
        result.setLnglat(formatLngLat(fenceHit.getCenterLng(), fenceHit.getCenterLat()));
        result.setCode(readStringProperty(fenceProperties, properties.getCodeProperty()));
        result.setAddress(geocode.getFormattedAddress());
        result.setAoi(aoi);
        result.setFenceInfo(buildFenceInfo(fenceHit, fenceProperties));

        writeCache(cacheKey, result);
        return result;
    }

    /**
     * 用经纬度坐标查询 AOI 几何：BP 优先（查 lbs_aoi_bp），命中即返回；未命中查 BUILDING（lbs_aoi_building）；都未命中返回 null。
     */
    private Object buildAoi(double lng, double lat) {
        String geojson = jituAoiMapper.findFirstBpGeojsonByPoint(lng, lat);
        if (geojson == null) {
            geojson = jituAoiMapper.findFirstBuildingGeojsonByPoint(lng, lat);
        }
        return readJsonObject(geojson);
    }

    private String resolveLayerId() {
        String layerId = properties.getLayerId();
        if (!StringUtils.hasText(layerId)) {
            throw BizException.badRequest("未配置围栏图层 ID（jitu.geo.layer-id）");
        }
        return layerId;
    }

    private Map<String, Object> buildFenceInfo(JituFenceHit fenceHit,
                                               Map<String, Object> fenceProperties) {
        Map<String, Object> fenceInfo = new LinkedHashMap<>();
        fenceInfo.put("featureId", fenceHit.getId());
        fenceInfo.put("geojson", readJsonObject(fenceHit.getGeometry()));
        fenceInfo.put("properties", filterFenceProperties(fenceProperties));
        return fenceInfo;
    }

    /**
     * 仅保留白名单字段（code/name/type/aoi_id），其他字段过滤掉。
     * 用 LinkedHashMap 保持白名单顺序。
     */
    private Map<String, Object> filterFenceProperties(Map<String, Object> source) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String key : FENCE_PROPERTY_WHITELIST) {
            if (source.containsKey(key)) {
                filtered.put(key, source.get(key));
            }
        }
        return filtered;
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
