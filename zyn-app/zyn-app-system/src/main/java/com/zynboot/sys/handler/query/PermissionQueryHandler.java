package com.zynboot.sys.handler.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.sys.response.permission.MenuTreeRes;
import com.zynboot.sys.infrastructure.entity.SysPermission;
import com.zynboot.sys.infrastructure.entity.SysRole;
import com.zynboot.sys.infrastructure.entity.SysRolePermission;
import com.zynboot.sys.infrastructure.entity.SysUserRole;
import com.zynboot.sys.infrastructure.mapper.SysPermissionMapper;
import com.zynboot.sys.infrastructure.mapper.SysRoleMapper;
import com.zynboot.sys.infrastructure.mapper.SysRolePermissionMapper;
import com.zynboot.sys.infrastructure.mapper.SysUserRoleMapper;
import com.zynboot.sys.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限查询处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionQueryHandler {

    private static final String CACHE_PERM_CODES_KEY = "sys:perm:user:%s";
    private static final String CACHE_ROLE_CODES_KEY = "sys:role:user:%s";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final CacheHelper cacheHelper;

    public Set<String> getPermCodes(String userId) {
        return cacheHelper.getOrLoad(CACHE_PERM_CODES_KEY.formatted(userId), CACHE_TTL, () ->
                new HashSet<>(permissionMapper.selectPermCodesByUserId(userId)));
    }

    public List<SysPermission> getPermsByUserId(String userId) {
        return permissionMapper.selectPermsByUserId(userId);
    }

    public Set<String> getRoleCodes(String userId) {
        return cacheHelper.getOrLoad(CACHE_ROLE_CODES_KEY.formatted(userId), CACHE_TTL, () ->
                roleMapper.selectRolesByUserId(userId).stream()
                        .map(SysRole::getRoleCode)
                        .collect(Collectors.toSet()));
    }

    public List<MenuTreeRes> getPermissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getPermType, 1, 2)
                        .eq(SysPermission::getStatus, 1)
                        .orderByAsc(SysPermission::getSort)
        );
        return buildTree(all, "0");
    }

    private List<MenuTreeRes> buildTree(List<SysPermission> all, String parentId) {
        Map<String, List<SysPermission>> parentMap = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getParentId() == null ? "0" : p.getParentId(),
                        Collectors.toList()
                ));
        return parentMap.getOrDefault(parentId, List.of()).stream()
                .map(p -> MenuTreeRes.builder()
                        .id(p.getId())
                        .parentId(p.getParentId())
                        .permName(p.getPermName())
                        .permType(p.getPermType())
                        .path(p.getPath())
                        .sort(p.getSort())
                        .visible(p.getVisible())
                        .children(buildTree(all, p.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    public void clearCache(String userId) {
        cacheHelper.evict(
                CACHE_PERM_CODES_KEY.formatted(userId),
                CACHE_ROLE_CODES_KEY.formatted(userId));
    }

    public void clearCacheByRoleId(String roleId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        userRoles.forEach(ur -> clearCache(ur.getUserId()));
    }

    public void clearCacheByPermissionId(String permissionId) {
        List<SysRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, permissionId));
        rolePerms.forEach(rp -> clearCacheByRoleId(rp.getRoleId()));
    }
}
