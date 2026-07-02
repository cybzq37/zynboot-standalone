package com.zynboot.map.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapFeature;
import com.zynboot.map.infrastructure.entity.MapLayerSource;
import com.zynboot.map.infrastructure.entity.MapSourceRaster;
import com.zynboot.map.infrastructure.entity.MapSourceTile;
import com.zynboot.map.infrastructure.entity.MapSourceProxy;
import com.zynboot.map.infrastructure.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SourceRepositoryImpl implements SourceRepository {

    private final MapLayerSourceMapper mapper;
    private final MapSourceRasterMapper rasterMapper;
    private final MapSourceTileMapper tileMapper;
    private final MapSourceProxyMapper proxyMapper;
    private final MapFeatureMapper featureMapper;

    @Override
    public List<SourceAggregate> findByLayerId(String layerId) {
        return mapper.selectList(
                new LambdaQueryWrapper<MapLayerSource>()
                        .eq(MapLayerSource::getLayerId, layerId)
                        .orderByDesc(MapLayerSource::getCreateTime))
                .stream().map(SourceAggregate::from).toList();
    }

    @Override
    public Optional<SourceAggregate> findById(String id) {
        MapLayerSource entity = mapper.selectById(id);
        return entity != null ? Optional.of(SourceAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public void save(SourceAggregate source) {
        mapper.insert(source.getEntity());
    }

    @Override
    public void update(SourceAggregate source) {
        mapper.updateById(source.getEntity());
    }

    @Override
    @Transactional
    public void delete(String id) {
        // 级联删除：feature → 子表 → source
        featureMapper.delete(
                new LambdaQueryWrapper<MapFeature>().eq(MapFeature::getSourceId, id));
        rasterMapper.deleteById(id);
        tileMapper.deleteById(id);
        proxyMapper.deleteById(id);
        mapper.deleteById(id);
    }
}
