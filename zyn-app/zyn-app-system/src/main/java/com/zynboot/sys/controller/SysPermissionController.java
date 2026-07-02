package com.zynboot.sys.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.kit.util.BeanUtils;
import com.zynboot.sys.command.permission.PermissionSaveCmd;
import com.zynboot.sys.domain.repository.PermissionRepository;
import com.zynboot.sys.handler.query.PermissionQueryHandler;
import com.zynboot.sys.infrastructure.entity.SysPermission;
import com.zynboot.sys.query.permission.PermissionQuery;
import com.zynboot.sys.response.permission.MenuTreeRes;
import com.zynboot.sys.response.permission.PermissionRes;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permission")
public class SysPermissionController {

    private final PermissionQueryHandler permissionQueryHandler;
    private final PermissionRepository permissionRepository;

    @GetMapping
    @SaCheckPermission("system:perm:query")
    public ApiResponse<List<PermissionRes>> list(PermissionQuery query) {
        return ApiResponse.ok(BeanUtils.copyList(permissionRepository.findList(query), PermissionRes.class));
    }

    @GetMapping("/tree")
    @SaCheckPermission("system:perm:query")
    public ApiResponse<List<MenuTreeRes>> tree() {
        return ApiResponse.ok(permissionQueryHandler.getPermissionTree());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:perm:query")
    public ApiResponse<PermissionRes> getById(@PathVariable String id) {
        SysPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("权限不存在"));
        return ApiResponse.ok(BeanUtils.copy(permission, PermissionRes.class));
    }

    @PostMapping
    @SaCheckPermission("system:perm:create")
    public ApiResponse<Void> create(@Valid @RequestBody PermissionSaveCmd cmd) {
        SysPermission permission = BeanUtils.copy(cmd, SysPermission.class);
        permissionRepository.save(permission);
        permissionQueryHandler.clearCacheByPermissionId(permission.getId());
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:perm:update")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody PermissionSaveCmd cmd) {
        SysPermission permission = BeanUtils.copy(cmd, SysPermission.class);
        permission.setId(id);
        permissionRepository.update(permission);
        permissionQueryHandler.clearCacheByPermissionId(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:perm:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        permissionQueryHandler.clearCacheByPermissionId(id);
        permissionRepository.delete(id);
        return ApiResponse.ok(null);
    }
}
