package com.zynboot.sys.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.kit.util.BeanUtils;
import com.zynboot.sys.command.org.OrgSaveCmd;
import com.zynboot.sys.domain.aggregate.OrgAggregate;
import com.zynboot.sys.domain.repository.OrgRepository;
import com.zynboot.sys.handler.query.OrgQueryHandler;
import com.zynboot.sys.response.org.OrgRes;
import com.zynboot.sys.response.org.OrgTreeRes;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/org")
public class SysOrganizationController {

    private final OrgQueryHandler orgQueryHandler;
    private final OrgRepository orgRepository;

    @GetMapping
    @SaCheckPermission("system:org:query")
    public ApiResponse<List<OrgRes>> list() {
        return ApiResponse.ok(
                orgRepository.findAll().stream()
                        .map(o -> BeanUtils.copy(o, OrgRes.class))
                        .toList()
        );
    }

    @GetMapping("/tree")
    @SaCheckPermission("system:org:query")
    public ApiResponse<List<OrgTreeRes>> tree() {
        return ApiResponse.ok(orgQueryHandler.getOrgTree());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:org:query")
    public ApiResponse<OrgRes> getById(@PathVariable String id) {
        OrgAggregate org = orgRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
        return ApiResponse.ok(BeanUtils.copy(org, OrgRes.class));
    }

    @PostMapping
    @SaCheckPermission("system:org:create")
    public ApiResponse<Void> create(@Valid @RequestBody OrgSaveCmd cmd) {
        OrgAggregate org = OrgAggregate.create(cmd.getOrgCode(), cmd.getOrgName(), cmd.getOrgType());
        org.setParentId(cmd.getParentId());
        org.updateInfo(cmd.getOrgName(), cmd.getPhone(), cmd.getEmail(), cmd.getRemark());
        orgRepository.save(org);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:org:update")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody OrgSaveCmd cmd) {
        OrgAggregate org = orgRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
        org.updateInfo(cmd.getOrgName(), cmd.getPhone(), cmd.getEmail(), cmd.getRemark());
        orgRepository.update(org);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:org:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        orgRepository.delete(id);
        return ApiResponse.ok(null);
    }
}
