package com.zynboot.map.domain.repository;

import com.zynboot.map.domain.aggregate.LayerGroupAggregate;

import java.util.List;
import java.util.Optional;

public interface LayerGroupRepository {
    List<LayerGroupAggregate> findAll();
    Optional<LayerGroupAggregate> findById(String id);
    void save(LayerGroupAggregate group);
    void update(LayerGroupAggregate group);
    void delete(String id);
}
