package com.zynboot.sys.domain.aggregate;

import com.zynboot.sys.infrastructure.entity.SysOrganization;

/**
 * 组织聚合根。
 */
public class OrgAggregate {

    private final SysOrganization entity;

    private OrgAggregate(SysOrganization entity) {
        this.entity = entity;
    }

    public static OrgAggregate from(SysOrganization entity) {
        return new OrgAggregate(entity);
    }

    public static OrgAggregate create(String orgCode, String orgName, Integer orgType) {
        SysOrganization org = new SysOrganization();
        org.setOrgCode(orgCode);
        org.setOrgName(orgName);
        org.setOrgType(orgType);
        org.setStatus(1);
        return new OrgAggregate(org);
    }

    /** 供 Repository 层持久化使用，禁止业务层调用。 */
    public SysOrganization toEntity() {
        return entity;
    }

    public String getId() {
        return entity.getId();
    }

    public String getOrgCode() {
        return entity.getOrgCode();
    }

    public String getOrgName() {
        return entity.getOrgName();
    }

    public Integer getOrgType() {
        return entity.getOrgType();
    }

    public Integer getSort() {
        return entity.getSort();
    }

    public Integer getStatus() {
        return entity.getStatus();
    }

    public void setParentId(String parentId) {
        entity.setParentId(parentId);
    }

    public void updateInfo(String orgName, String phone, String email, String remark) {
        if (orgName != null) entity.setOrgName(orgName);
        if (phone != null) entity.setPhone(phone);
        if (email != null) entity.setEmail(email);
        if (remark != null) entity.setRemark(remark);
    }
}
