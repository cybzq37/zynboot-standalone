package com.zynboot.sys.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.kit.util.BeanUtils;
import com.zynboot.sys.command.role.RoleSaveCmd;
import com.zynboot.sys.domain.aggregate.RoleAggregate;
import com.zynboot.sys.domain.repository.RoleRepository;
import com.zynboot.sys.handler.query.PermissionQueryHandler;
import com.zynboot.sys.handler.query.RoleQueryHandler;
import com.zynboot.sys.query.role.RoleQuery;
import com.zynboot.sys.response.role.RoleRes;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/role")
public class SysRoleController {

    private final RoleQueryHandler roleQueryHandler;
    private final PermissionQueryHandler permissionQueryHandler;
    private final RoleRepository roleRepository;

    @GetMapping
    @SaCheckPermission("system:role:query")
    public ApiResponse<List<RoleRes>> list(RoleQuery query) {
        return ApiResponse.ok(BeanUtils.copyList(roleRepository.findList(query), RoleRes.class));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:role:query")
    public ApiResponse<RoleRes> getById(@PathVariable String id) {
        return ApiResponse.ok(roleQueryHandler.findById(id));
    }

    @PostMapping
    @SaCheckPermission("system:role:create")
    public ApiResponse<Void> create(@Valid @RequestBody RoleSaveCmd cmd) {
        RoleAggregate role = RoleAggregate.create(cmd.getRoleCode(), cmd.getRoleName());
        role.updateInfo(cmd.getRoleName(), cmd.getDataScope(), cmd.getRemark());
        roleRepository.save(role);
        permissionQueryHandler.clearCacheByRoleId(role.getId());
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:role:update")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody RoleSaveCmd cmd) {
        RoleAggregate role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        role.updateInfo(cmd.getRoleName(), cmd.getDataScope(), cmd.getRemark());
        roleRepository.update(role);
        permissionQueryHandler.clearCacheByRoleId(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:role:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        permissionQueryHandler.clearCacheByRoleId(id);
        roleRepository.delete(id);
        return ApiResponse.ok(null);
    }
}
