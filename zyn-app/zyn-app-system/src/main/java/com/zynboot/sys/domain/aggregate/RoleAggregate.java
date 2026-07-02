package com.zynboot.sys.domain.aggregate;

import com.zynboot.sys.infrastructure.entity.SysRole;

/**
 * 角色聚合根。
 */
public class RoleAggregate {

    private final SysRole entity;

    private RoleAggregate(SysRole entity) {
        this.entity = entity;
    }

    public static RoleAggregate from(SysRole entity) {
        return new RoleAggregate(entity);
    }

    public static RoleAggregate create(String roleCode, String roleName) {
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setStatus(1);
        return new RoleAggregate(role);
    }

    /** 供 Repository 层持久化使用，禁止业务层调用。 */
    public SysRole toEntity() {
        return entity;
    }

    public String getId() {
        return entity.getId();
    }

    public String getRoleCode() {
        return entity.getRoleCode();
    }

    public String getRoleName() {
        return entity.getRoleName();
    }

    public Integer getDataScope() {
        return entity.getDataScope();
    }

    public Integer getSort() {
        return entity.getSort();
    }

    public Integer getStatus() {
        return entity.getStatus();
    }

    public String getRemark() {
        return entity.getRemark();
    }

    public void updateInfo(String roleName, Integer dataScope, String remark) {
        if (roleName != null) entity.setRoleName(roleName);
        if (dataScope != null) entity.setDataScope(dataScope);
        if (remark != null) entity.setRemark(remark);
    }
}
