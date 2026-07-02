package com.zynboot.sys.domain.aggregate;

import com.zynboot.sys.infrastructure.entity.SysRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAggregateTest {

    @Test
    void shouldCreateRoleWithDefaultStatus() {
        RoleAggregate role = RoleAggregate.create("ADMIN", "管理员");

        assertThat(role.getRoleCode()).isEqualTo("ADMIN");
        assertThat(role.getRoleName()).isEqualTo("管理员");
        assertThat(role.getStatus()).isEqualTo(1);
    }

    @Test
    void shouldReconstituteFromExistingEntity() {
        SysRole entity = new SysRole();
        entity.setId("r-001");
        entity.setRoleCode("USER");

        RoleAggregate role = RoleAggregate.from(entity);

        assertThat(role.getId()).isEqualTo("r-001");
        assertThat(role.getRoleCode()).isEqualTo("USER");
    }

    @Test
    void shouldUpdateInfoWithNonNullFieldsOnly() {
        RoleAggregate role = RoleAggregate.create("ADMIN", "管理员");

        role.updateInfo("超级管理员", 1, "系统内置角色");

        assertThat(role.getRoleName()).isEqualTo("超级管理员");
        assertThat(role.getDataScope()).isEqualTo(1);
        assertThat(role.getRemark()).isEqualTo("系统内置角色");
    }

    @Test
    void shouldNotOverwriteWithNullOnUpdateInfo() {
        RoleAggregate role = RoleAggregate.create("ADMIN", "管理员");
        role.updateInfo("超级管理员", 1, "备注");

        role.updateInfo(null, 3, null);

        assertThat(role.getRoleName()).isEqualTo("超级管理员");
        assertThat(role.getDataScope()).isEqualTo(3);
        assertThat(role.getRemark()).isEqualTo("备注");
    }
}
