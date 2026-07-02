package com.zynboot.map.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.map.domain.aggregate.LayerAggregate;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.infrastructure.entity.MapLayer;
import com.zynboot.map.infrastructure.mapper.MapLayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LayerRepositoryImpl implements LayerRepository {

    private final MapLayerMapper mapper;

    @Override
    public List<LayerAggregate> findByGroupId(String groupId) {
        LambdaQueryWrapper<MapLayer> wrapper = new LambdaQueryWrapper<MapLayer>()
                .eq(StringUtils.hasText(groupId), MapLayer::getGroupId, groupId)
                .orderByAsc(MapLayer::getRenderOrder);
        return mapper.selectList(wrapper).stream().map(LayerAggregate::from).toList();
    }

    @Override
    public Optional<LayerAggregate> findById(String id) {
        MapLayer entity = mapper.selectById(id);
        return entity != null ? Optional.of(LayerAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public void save(LayerAggregate layer) {
        mapper.insert(layer.getEntity());
    }

    @Override
    public void update(LayerAggregate layer) {
        mapper.updateById(layer.getEntity());
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }
}
