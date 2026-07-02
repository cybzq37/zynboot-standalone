package com.zynboot.map.domain.repository;

import com.zynboot.map.domain.aggregate.LayerAggregate;

import java.util.List;
import java.util.Optional;

public interface LayerRepository {
    List<LayerAggregate> findByGroupId(String groupId);
    Optional<LayerAggregate> findById(String id);
    void save(LayerAggregate layer);
    void update(LayerAggregate layer);
    void delete(String id);
}
