package com.zynboot.sys.handler.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.sys.response.org.OrgTreeRes;
import com.zynboot.sys.infrastructure.entity.SysOrganization;
import com.zynboot.sys.infrastructure.mapper.SysOrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织查询处理器。
 */
@Component
@RequiredArgsConstructor
public class OrgQueryHandler {

    private final SysOrganizationMapper organizationMapper;

    public List<OrgTreeRes> getOrgTree() {
        List<SysOrganization> all = organizationMapper.selectList(
                new LambdaQueryWrapper<SysOrganization>()
                        .eq(SysOrganization::getStatus, 1)
                        .orderByAsc(SysOrganization::getSort)
        );
        return buildTree(all, "0");
    }

    private List<OrgTreeRes> buildTree(List<SysOrganization> all, String parentId) {
        Map<String, List<SysOrganization>> parentMap = all.stream()
                .collect(Collectors.groupingBy(
                        org -> org.getParentId() == null ? "0" : org.getParentId(),
                        Collectors.toList()
                ));
        return parentMap.getOrDefault(parentId, List.of()).stream()
                .map(org -> OrgTreeRes.builder()
                        .id(org.getId())
                        .parentId(org.getParentId())
                        .orgCode(org.getOrgCode())
                        .orgName(org.getOrgName())
                        .orgType(org.getOrgType())
                        .sort(org.getSort())
                        .children(buildTree(all, org.getId()))
                        .build())
                .collect(Collectors.toList());
    }
}
