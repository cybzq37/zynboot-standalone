package com.zynboot.sys.domain.repository;

import com.zynboot.sys.infrastructure.entity.SysPermission;
import com.zynboot.sys.query.permission.PermissionQuery;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    List<SysPermission> findList(PermissionQuery query);

    Optional<SysPermission> findById(String id);

    void save(SysPermission permission);

    void update(SysPermission permission);

    void delete(String id);
}
