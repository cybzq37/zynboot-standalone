package com.zynboot.sys.domain.repository;

import com.zynboot.sys.domain.aggregate.OrgAggregate;
import java.util.List;
import java.util.Optional;

/**
 * 组织仓储接口。
 */
public interface OrgRepository {

    Optional<OrgAggregate> findById(String id);

    Optional<OrgAggregate> findByCode(String orgCode);

    List<OrgAggregate> findAll();

    void save(OrgAggregate org);

    void update(OrgAggregate org);

    void delete(String id);
}
