package com.zynboot.sys.domain.aggregate;

import com.zynboot.sys.infrastructure.entity.SysOrganization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrgAggregateTest {

    @Test
    void shouldCreateOrgWithDefaultStatus() {
        OrgAggregate org = OrgAggregate.create("TECH", "技术部", 1);

        assertThat(org.getOrgCode()).isEqualTo("TECH");
        assertThat(org.getOrgName()).isEqualTo("技术部");
        assertThat(org.getOrgType()).isEqualTo(1);
        assertThat(org.getStatus()).isEqualTo(1);
    }

    @Test
    void shouldReconstituteFromExistingEntity() {
        SysOrganization entity = new SysOrganization();
        entity.setId("o-001");
        entity.setOrgCode("HR");

        OrgAggregate org = OrgAggregate.from(entity);

        assertThat(org.getId()).isEqualTo("o-001");
        assertThat(org.getOrgCode()).isEqualTo("HR");
    }

    @Test
    void shouldSetParentId() {
        OrgAggregate org = OrgAggregate.create("CHILD", "子部门", 1);

        org.setParentId("parent-001");

        assertThat(org.toEntity().getParentId()).isEqualTo("parent-001");
    }

    @Test
    void shouldUpdateInfoWithNonNullFieldsOnly() {
        OrgAggregate org = OrgAggregate.create("TECH", "技术部", 1);

        org.updateInfo("研发中心", "123456", "a@b.com", "备注");

        assertThat(org.getOrgName()).isEqualTo("研发中心");
        assertThat(org.toEntity().getPhone()).isEqualTo("123456");
        assertThat(org.toEntity().getEmail()).isEqualTo("a@b.com");
        assertThat(org.toEntity().getRemark()).isEqualTo("备注");
    }

    @Test
    void shouldNotOverwriteWithNullOnUpdateInfo() {
        OrgAggregate org = OrgAggregate.create("TECH", "技术部", 1);
        org.updateInfo("研发中心", "123456", "a@b.com", "备注");

        org.updateInfo("新名称", null, null, "新备注");

        assertThat(org.getOrgName()).isEqualTo("新名称");
        assertThat(org.toEntity().getPhone()).isEqualTo("123456");
        assertThat(org.toEntity().getEmail()).isEqualTo("a@b.com");
        assertThat(org.toEntity().getRemark()).isEqualTo("新备注");
    }
}
