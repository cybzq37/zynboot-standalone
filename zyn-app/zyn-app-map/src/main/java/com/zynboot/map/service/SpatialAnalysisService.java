package com.zynboot.map.service;

import com.zynboot.map.infrastructure.mapper.MapSpatialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 空间分析服务（基于 PostGIS）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpatialAnalysisService {

    private final MapSpatialMapper spatialMapper;

    /**
     * 缓冲区分析：对指定要素生成缓冲区，返回 GeoJSON。
     *
     * @param featureId 要素 ID
     * @param distanceMeters 缓冲距离（米）
     * @return 缓冲区 GeoJSON
     */
    public String buffer(String featureId, double distanceMeters) {
        return spatialMapper.buffer(featureId, distanceMeters);
    }

    /**
     * 距离量算：两个要素之间的距离（米）。
     */
    public double distance(String featureId1, String featureId2) {
        return spatialMapper.distance(featureId1, featureId2);
    }

    /**
     * 面积量算：计算要素面积（平方米）。
     */
    public double area(String featureId) {
        return spatialMapper.area(featureId);
    }

    /**
     * 空间关系判断：两个要素是否相交。
     */
    public boolean intersects(String featureId1, String featureId2) {
        return spatialMapper.intersects(featureId1, featureId2);
    }

    /**
     * 叠加分析：两个图层的交集。
     *
     * @param layerId1 图层 1
     * @param layerId2 图层 2
     * @return 交集要素列表
     */
    public List<Map<String, Object>> intersection(String layerId1, String layerId2) {
        return spatialMapper.intersection(layerId1, layerId2);
    }
}
