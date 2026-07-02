package com.zynboot.sys.handler.query;

import com.zynboot.sys.response.user.LoginUserRes;
import com.zynboot.sys.response.user.UserRes;
import com.zynboot.sys.infrastructure.entity.SysRole;
import com.zynboot.sys.infrastructure.entity.SysUser;
import com.zynboot.sys.infrastructure.mapper.SysPermissionMapper;
import com.zynboot.sys.infrastructure.mapper.SysRoleMapper;
import com.zynboot.sys.infrastructure.mapper.SysUserMapper;
import com.zynboot.sys.util.CacheHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户查询处理器（读操作，直接调 Mapper）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserQueryHandler {

    private static final String CACHE_USER_KEY = "sys:user:login:%s";
    private static final String CACHE_PERM_KEY = "sys:perm:user:%s";
    private static final String CACHE_ROLE_KEY = "sys:role:user:%s";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final CacheHelper cacheHelper;

    public UserRes findById(String id) {
        SysUser entity = userMapper.selectById(id);
        return entity != null ? toRes(entity) : null;
    }

    public UserRes findByUsername(String username) {
        SysUser entity = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return entity != null ? toRes(entity) : null;
    }

    public LoginUserRes getLoginUser(String userId) {
        return cacheHelper.getOrLoad(CACHE_USER_KEY.formatted(userId), CACHE_TTL, () -> {
            SysUser user = userMapper.selectById(userId);
            if (user == null) return null;

            Set<String> roleCodes = getRoleCodes(userId);
            Set<String> permCodes = getPermCodes(userId);

            return LoginUserRes.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .realName(user.getRealName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .status(user.getStatus())
                    .roleCodes(roleCodes)
                    .permCodes(permCodes)
                    .build();
        });
    }

    public Set<String> getPermCodes(String userId) {
        return cacheHelper.getOrLoad(CACHE_PERM_KEY.formatted(userId), CACHE_TTL, () ->
                new java.util.HashSet<>(permissionMapper.selectPermCodesByUserId(userId)));
    }

    public Set<String> getRoleCodes(String userId) {
        return cacheHelper.getOrLoad(CACHE_ROLE_KEY.formatted(userId), CACHE_TTL, () ->
                roleMapper.selectRolesByUserId(userId).stream()
                        .map(SysRole::getRoleCode)
                        .collect(Collectors.toSet()));
    }

    public void clearCache(String userId) {
        cacheHelper.evict(
                CACHE_USER_KEY.formatted(userId),
                CACHE_PERM_KEY.formatted(userId),
                CACHE_ROLE_KEY.formatted(userId));
    }

    private UserRes toRes(SysUser entity) {
        UserRes res = new UserRes();
        res.setId(entity.getId());
        res.setUsername(entity.getUsername());
        res.setNickname(entity.getNickname());
        res.setRealName(entity.getRealName());
        res.setEmail(entity.getEmail());
        res.setPhone(entity.getPhone());
        res.setAvatar(entity.getAvatar());
        res.setGender(entity.getGender());
        res.setStatus(entity.getStatus());
        return res;
    }
}
