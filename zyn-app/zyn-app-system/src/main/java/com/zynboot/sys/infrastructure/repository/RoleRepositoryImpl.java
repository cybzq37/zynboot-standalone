package com.zynboot.sys.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.sys.domain.aggregate.RoleAggregate;
import com.zynboot.sys.domain.repository.RoleRepository;
import com.zynboot.sys.infrastructure.entity.SysRole;
import com.zynboot.sys.infrastructure.entity.SysRolePermission;
import com.zynboot.sys.infrastructure.entity.SysUserRole;
import com.zynboot.sys.infrastructure.mapper.SysRoleMapper;
import com.zynboot.sys.infrastructure.mapper.SysRolePermissionMapper;
import com.zynboot.sys.infrastructure.mapper.SysUserRoleMapper;
import com.zynboot.sys.query.role.RoleQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final SysRoleMapper mapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public Optional<RoleAggregate> findById(String id) {
        SysRole entity = mapper.selectById(id);
        return entity != null ? Optional.of(RoleAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public Optional<RoleAggregate> findByCode(String roleCode) {
        SysRole entity = mapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        return entity != null ? Optional.of(RoleAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public List<RoleAggregate> findList(RoleQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(query.getRoleCode()), SysRole::getRoleCode, query.getRoleCode())
                .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSort);
        return mapper.selectList(wrapper).stream().map(RoleAggregate::from).toList();
    }

    @Override
    public void save(RoleAggregate role) {
        mapper.insert(role.toEntity());
    }

    @Override
    public void update(RoleAggregate role) {
        mapper.updateById(role.toEntity());
    }

    @Override
    public void delete(String id) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        mapper.deleteById(id);
    }
}
