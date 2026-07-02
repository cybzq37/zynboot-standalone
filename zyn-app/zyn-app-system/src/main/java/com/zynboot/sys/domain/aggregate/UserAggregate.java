package com.zynboot.sys.domain.aggregate;

import com.zynboot.sys.domain.enums.UserStatus;
import com.zynboot.sys.infrastructure.entity.SysUser;

import java.time.LocalDateTime;

/**
 * 用户聚合根，封装用户相关的业务规则。
 */
public class UserAggregate {

    /** 用户资料值对象，群组化 updateProfile 参数。 */
    public record UserProfileVO(String nickname, String realName, String email,
                                String phone, String avatar, Integer gender, String remark) {}

    private final SysUser entity;

    private UserAggregate(SysUser entity) {
        this.entity = entity;
    }

    public static UserAggregate from(SysUser entity) {
        return new UserAggregate(entity);
    }

    public static UserAggregate create(String username, String password) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setLoginAttempts(0);
        return new UserAggregate(user);
    }

    /** 供 Repository 层持久化使用，禁止业务层调用。 */
    public SysUser toEntity() {
        return entity;
    }

    public String getId() {
        return entity.getId();
    }

    public String getUsername() {
        return entity.getUsername();
    }

    public String getPassword() {
        return entity.getPassword();
    }

    public String getNickname() {
        return entity.getNickname();
    }

    public String getRealName() {
        return entity.getRealName();
    }

    public String getEmail() {
        return entity.getEmail();
    }

    public String getPhone() {
        return entity.getPhone();
    }

    public String getAvatar() {
        return entity.getAvatar();
    }

    public Integer getGender() {
        return entity.getGender();
    }

    public Integer getStatus() {
        return entity.getStatus();
    }

    public String getRemark() {
        return entity.getRemark();
    }

    public void updateProfile(UserProfileVO profile) {
        if (profile.nickname() != null) entity.setNickname(profile.nickname());
        if (profile.realName() != null) entity.setRealName(profile.realName());
        if (profile.email() != null) entity.setEmail(profile.email());
        if (profile.phone() != null) entity.setPhone(profile.phone());
        if (profile.avatar() != null) entity.setAvatar(profile.avatar());
        if (profile.gender() != null) entity.setGender(profile.gender());
        if (profile.remark() != null) entity.setRemark(profile.remark());
    }

    /** 重载：兼容旧调用（逐步迁移到 UserProfileVO）。 */
    public void updateProfile(String nickname, String realName, String email,
                              String phone, String avatar, Integer gender, String remark) {
        updateProfile(new UserProfileVO(nickname, realName, email, phone, avatar, gender, remark));
    }

    public void updatePassword(String encodedPassword) {
        entity.setPassword(encodedPassword);
        entity.setPwdUpdateTime(LocalDateTime.now());
    }

    public void recordLoginSuccess(String ip) {
        entity.setLoginIp(ip);
        entity.setLoginTime(LocalDateTime.now());
        entity.setLoginAttempts(0);
        entity.setLockTime(null);
    }

    public void recordLoginFailure(int maxAttempts) {
        int attempts = entity.getLoginAttempts() == null ? 0 : entity.getLoginAttempts();
        entity.setLoginAttempts(attempts + 1);
        if (attempts + 1 >= maxAttempts) {
            entity.setLockTime(LocalDateTime.now());
        }
    }

    /**
     * 手动锁定账户（管理员操作），记录锁定时间。
     */
    public void lock() {
        entity.setLockTime(LocalDateTime.now());
    }

    /**
     * 手动解锁账户，清除锁定状态与失败计数。
     */
    public void unlock() {
        entity.setLockTime(null);
        entity.setLoginAttempts(0);
    }

    public void disable() {
        entity.setStatus(UserStatus.DISABLED.getCode());
    }

    public void enable() {
        entity.setStatus(UserStatus.NORMAL.getCode());
        entity.setLoginAttempts(0);
        entity.setLockTime(null);
    }

    /**
     * 保留用户名（注册时预占），状态为待激活。
     */
    public void reserve() {
        entity.setStatus(UserStatus.DISABLED.getCode());
    }

    public boolean isLocked() {
        return entity.getLockTime() != null;
    }

    public boolean isDisabled() {
        return Integer.valueOf(UserStatus.DISABLED.getCode()).equals(entity.getStatus());
    }
}
