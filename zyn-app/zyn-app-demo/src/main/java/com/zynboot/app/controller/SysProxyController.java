package com.zynboot.app.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.api.*;
import com.zynboot.sys.command.user.LoginCmd;
import com.zynboot.sys.command.org.OrgSaveCmd;
import com.zynboot.sys.command.permission.PermissionSaveCmd;
import com.zynboot.sys.command.resource.ResourceSaveCmd;
import com.zynboot.sys.command.role.RoleSaveCmd;
import com.zynboot.sys.command.user.UserSaveCmd;
import com.zynboot.sys.query.org.OrgQuery;
import com.zynboot.sys.query.permission.PermissionQuery;
import com.zynboot.sys.query.role.RoleQuery;
import com.zynboot.sys.query.user.UserPageQuery;
import com.zynboot.sys.response.org.OrgRes;
import com.zynboot.sys.response.org.OrgTreeRes;
import com.zynboot.sys.response.permission.MenuTreeRes;
import com.zynboot.sys.response.permission.PermissionRes;
import com.zynboot.sys.response.resource.ResourceRes;
import com.zynboot.sys.response.role.RoleRes;
import com.zynboot.sys.response.user.LoginRes;
import com.zynboot.sys.response.user.UserInfoRes;
import com.zynboot.sys.response.PageRes;
import com.zynboot.sys.response.user.UserRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统 API 代理端点 —— 用于验证跨服务 Remote 调用。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sys-proxy")
public class SysProxyController {

    private final SysAuthApi authApi;
    private final SysUserApi userApi;
    private final SysRoleApi roleApi;
    private final SysPermissionApi permissionApi;
    private final SysResourceApi resourceApi;
    private final SysOrganizationApi orgApi;

    // ── Auth ──────────────────────────────────────────────────
    @PostMapping("/auth/login")
    public ApiResponse<LoginRes> login(@RequestBody LoginCmd cmd) {
        return authApi.login(cmd);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout() {
        return authApi.logout();
    }

    @GetMapping("/auth/info")
    public ApiResponse<UserInfoRes> authInfo() {
        return authApi.info();
    }

    // ── User ──────────────────────────────────────────────────
    @GetMapping("/user")
    public ApiResponse<PageRes<UserRes>> userPage(UserPageQuery query) {
        return userApi.page(query);
    }

    @GetMapping("/user/{id}")
    public ApiResponse<UserRes> userGetById(@PathVariable String id) {
        return userApi.getById(id);
    }

    @PostMapping("/user")
    public ApiResponse<Void> userCreate(@RequestBody UserSaveCmd cmd) {
        return userApi.create(cmd);
    }

    @PutMapping("/user/{id}")
    public ApiResponse<Void> userUpdate(@PathVariable String id, @RequestBody UserSaveCmd cmd) {
        return userApi.update(id, cmd);
    }

    @DeleteMapping("/user/{id}")
    public ApiResponse<Void> userDelete(@PathVariable String id) {
        return userApi.delete(id);
    }

    // ── Role ──────────────────────────────────────────────────
    @GetMapping("/role")
    public ApiResponse<List<RoleRes>> roleList(RoleQuery query) {
        return roleApi.list(query);
    }

    @GetMapping("/role/{id}")
    public ApiResponse<RoleRes> roleGetById(@PathVariable String id) {
        return roleApi.getById(id);
    }

    @PostMapping("/role")
    public ApiResponse<Void> roleCreate(@RequestBody RoleSaveCmd cmd) {
        return roleApi.create(cmd);
    }

    @PutMapping("/role/{id}")
    public ApiResponse<Void> roleUpdate(@PathVariable String id, @RequestBody RoleSaveCmd cmd) {
        return roleApi.update(id, cmd);
    }

    @DeleteMapping("/role/{id}")
    public ApiResponse<Void> roleDelete(@PathVariable String id) {
        return roleApi.delete(id);
    }

    // ── Permission ────────────────────────────────────────────
    @GetMapping("/permission")
    public ApiResponse<List<PermissionRes>> permissionList(PermissionQuery query) {
        return permissionApi.list(query);
    }

    @GetMapping("/permission/tree")
    public ApiResponse<List<MenuTreeRes>> permissionTree() {
        return permissionApi.tree();
    }

    @GetMapping("/permission/{id}")
    public ApiResponse<PermissionRes> permissionGetById(@PathVariable String id) {
        return permissionApi.getById(id);
    }

    @PostMapping("/permission")
    public ApiResponse<Void> permissionCreate(@RequestBody PermissionSaveCmd cmd) {
        return permissionApi.create(cmd);
    }

    @PutMapping("/permission/{id}")
    public ApiResponse<Void> permissionUpdate(@PathVariable String id, @RequestBody PermissionSaveCmd cmd) {
        return permissionApi.update(id, cmd);
    }

    @DeleteMapping("/permission/{id}")
    public ApiResponse<Void> permissionDelete(@PathVariable String id) {
        return permissionApi.delete(id);
    }

    // ── Resource ──────────────────────────────────────────────
    @GetMapping("/resource")
    public ApiResponse<List<ResourceRes>> resourceList() {
        return resourceApi.list();
    }

    @GetMapping("/resource/{id}")
    public ApiResponse<ResourceRes> resourceGetById(@PathVariable String id) {
        return resourceApi.getById(id);
    }

    @PostMapping("/resource")
    public ApiResponse<Void> resourceCreate(@RequestBody ResourceSaveCmd cmd) {
        return resourceApi.create(cmd);
    }

    @PutMapping("/resource/{id}")
    public ApiResponse<Void> resourceUpdate(@PathVariable String id, @RequestBody ResourceSaveCmd cmd) {
        return resourceApi.update(id, cmd);
    }

    @DeleteMapping("/resource/{id}")
    public ApiResponse<Void> resourceDelete(@PathVariable String id) {
        return resourceApi.delete(id);
    }

    // ── Organization ──────────────────────────────────────────
    @GetMapping("/org")
    public ApiResponse<List<OrgRes>> orgList(OrgQuery query) {
        return orgApi.list();
    }

    @GetMapping("/org/tree")
    public ApiResponse<List<OrgTreeRes>> orgTree() {
        return orgApi.tree();
    }

    @GetMapping("/org/{id}")
    public ApiResponse<OrgRes> orgGetById(@PathVariable String id) {
        return orgApi.getById(id);
    }

    @PostMapping("/org")
    public ApiResponse<Void> orgCreate(@RequestBody OrgSaveCmd cmd) {
        return orgApi.create(cmd);
    }

    @PutMapping("/org/{id}")
    public ApiResponse<Void> orgUpdate(@PathVariable String id, @RequestBody OrgSaveCmd cmd) {
        return orgApi.update(id, cmd);
    }

    @DeleteMapping("/org/{id}")
    public ApiResponse<Void> orgDelete(@PathVariable String id) {
        return orgApi.delete(id);
    }
}
