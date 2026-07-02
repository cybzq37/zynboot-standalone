package com.zynboot.map.service;

import com.zynboot.map.infrastructure.entity.MapFeature;
import com.zynboot.map.infrastructure.mapper.MapFeatureMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureService {

    private final MapFeatureMapper featureMapper;

    public long countByLayerId(String layerId) {
        return featureMapper.countByLayerId(layerId);
    }

    public long countBySourceId(String sourceId) {
        return featureMapper.countBySourceId(sourceId);
    }
}
