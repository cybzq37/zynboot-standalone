package com.zynboot.jitu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.infra.redis.RedisClient;
import com.zynboot.jitu.config.JituGeoProperties;
import com.zynboot.jitu.integration.geo.UpstreamGeoClient;
import com.zynboot.jitu.integration.geo.UpstreamGeoResponse;
import com.zynboot.kit.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodeService {

    private final UpstreamGeoClient upstreamGeoClient;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final JituGeoProperties properties;

    public GeocodeCacheValue geocode(String address) {
        String normalizedAddress = normalizeAddress(address);
        String cacheKey = "jitu:geo:geocode:" + DigestUtils.md5DigestAsHex(normalizedAddress.getBytes(StandardCharsets.UTF_8));
        GeocodeCacheValue cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        UpstreamGeoResponse response = upstreamGeoClient.geocode(properties.getApiKey(), normalizedAddress);
        if (response == null) {
            throw BizException.badRequest("地址解析失败，未返回有效结果");
        }
        if (StringUtils.hasText(response.getStatus()) && !"0".equals(response.getStatus())) {
            throw BizException.badRequest("地址解析失败: " + response.getMessage());
        }
        if (response.getGeocodes() == null || response.getGeocodes().isEmpty()) {
            throw BizException.badRequest("地址解析失败，未返回有效坐标");
        }

        UpstreamGeoResponse.GeocodeItem first = response.getGeocodes().getFirst();
        if (!StringUtils.hasText(first.getLocation())) {
            throw BizException.badRequest("地址解析失败，缺少坐标信息");
        }

        String[] lngLat = first.getLocation().split(",");
        if (lngLat.length != 2) {
            throw BizException.badRequest("地址解析失败，坐标格式不正确");
        }

        GeocodeCacheValue value;
        try {
            value = new GeocodeCacheValue(
                    StringUtils.hasText(first.getFormattedAddress()) ? first.getFormattedAddress() : normalizedAddress,
                    Double.parseDouble(lngLat[0].trim()),
                    Double.parseDouble(lngLat[1].trim())
            );
        } catch (NumberFormatException ex) {
            throw BizException.badRequest("地址解析失败，坐标值不合法");
        }
        log.info("Geocode result: address='{}' -> lng={}, lat={}", normalizedAddress, value.getLng(), value.getLat());
        writeCache(cacheKey, value);
        return value;
    }

    private String normalizeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            throw BizException.badRequest("address 不能为空");
        }
        return address.trim();
    }

    private GeocodeCacheValue readCache(String key) {
        try {
            Optional<String> value = redisClient.get(key);
            if (value.isEmpty() || !StringUtils.hasText(value.get())) {
                return null;
            }
            return objectMapper.readValue(value.get(), GeocodeCacheValue.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeCache(String key, GeocodeCacheValue value) {
        try {
            redisClient.put(key, objectMapper.writeValueAsString(value), properties.getGeocodeCacheTtl());
        } catch (Exception ignored) {
            // cache is best effort
        }
    }
}
