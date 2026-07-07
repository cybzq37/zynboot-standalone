package com.zynboot.map.domain.repository;

import com.zynboot.map.domain.aggregate.SourceAggregate;

import java.util.List;
import java.util.Optional;

public interface SourceRepository {
    List<SourceAggregate> findByLayerId(String layerId);
    Optional<SourceAggregate> findById(String id);
    boolean existsByLayerId(String layerId);
    void save(SourceAggregate source);
    void update(SourceAggregate source);
    void delete(String id);
}
