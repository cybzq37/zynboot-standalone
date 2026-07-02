package com.zynboot.sys.handler.query;

import com.zynboot.sys.response.role.RoleRes;
import com.zynboot.sys.infrastructure.entity.SysRole;
import com.zynboot.sys.infrastructure.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色查询处理器。
 */
@Component
@RequiredArgsConstructor
public class RoleQueryHandler {

    private final SysRoleMapper roleMapper;

    public RoleRes findById(String id) {
        SysRole entity = roleMapper.selectById(id);
        return entity != null ? toRes(entity) : null;
    }

    public List<RoleRes> listAll() {
        return roleMapper.selectList(null).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public List<SysRole> getRolesByUserId(String userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    private RoleRes toRes(SysRole entity) {
        RoleRes res = new RoleRes();
        res.setId(entity.getId());
        res.setRoleCode(entity.getRoleCode());
        res.setRoleName(entity.getRoleName());
        res.setRoleType(entity.getRoleType());
        res.setDataScope(entity.getDataScope());
        res.setSort(entity.getSort());
        res.setStatus(entity.getStatus());
        res.setRemark(entity.getRemark());
        return res;
    }
}
