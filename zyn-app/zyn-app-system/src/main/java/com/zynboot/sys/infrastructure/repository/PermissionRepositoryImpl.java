package com.zynboot.sys.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.sys.domain.repository.PermissionRepository;
import com.zynboot.sys.infrastructure.entity.SysPermission;
import com.zynboot.sys.infrastructure.entity.SysRolePermission;
import com.zynboot.sys.infrastructure.mapper.SysPermissionMapper;
import com.zynboot.sys.infrastructure.mapper.SysRolePermissionMapper;
import com.zynboot.sys.query.permission.PermissionQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final SysPermissionMapper mapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<SysPermission> findList(PermissionQuery query) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .like(StringUtils.hasText(query.getPermName()), SysPermission::getPermName, query.getPermName())
                .eq(query.getPermType() != null, SysPermission::getPermType, query.getPermType())
                .eq(query.getStatus() != null, SysPermission::getStatus, query.getStatus())
                .orderByAsc(SysPermission::getSort);
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<SysPermission> findById(String id) {
        SysPermission entity = mapper.selectById(id);
        return Optional.ofNullable(entity);
    }

    @Override
    public void save(SysPermission permission) {
        mapper.insert(permission);
    }

    @Override
    public void update(SysPermission permission) {
        mapper.updateById(permission);
    }

    @Override
    public void delete(String id) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, id));
        mapper.deleteById(id);
    }
}
