package com.zynboot.map.infrastructure.repository;

import com.zynboot.map.domain.aggregate.LayerGroupAggregate;
import com.zynboot.map.domain.repository.LayerGroupRepository;
import com.zynboot.map.infrastructure.entity.MapLayerGroup;
import com.zynboot.map.infrastructure.mapper.MapLayerGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LayerGroupRepositoryImpl implements LayerGroupRepository {

    private final MapLayerGroupMapper mapper;

    @Override
    public List<LayerGroupAggregate> findAll() {
        return mapper.selectList(null).stream().map(LayerGroupAggregate::from).toList();
    }

    @Override
    public Optional<LayerGroupAggregate> findById(String id) {
        MapLayerGroup entity = mapper.selectById(id);
        return entity != null ? Optional.of(LayerGroupAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public void save(LayerGroupAggregate group) {
        mapper.insert(group.getEntity());
    }

    @Override
    public void update(LayerGroupAggregate group) {
        mapper.updateById(group.getEntity());
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }
}
