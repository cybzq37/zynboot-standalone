package com.zynboot.sys.domain.aggregate;

import com.zynboot.sys.domain.enums.UserStatus;
import com.zynboot.sys.infrastructure.entity.SysUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAggregateTest {

    @Test
    void shouldCreateUserWithDefaultStatusAndZeroAttempts() {
        UserAggregate user = UserAggregate.create("admin", "encoded_pwd");

        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getPassword()).isEqualTo("encoded_pwd");
        assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL.getCode());
        assertThat(user.toEntity().getLoginAttempts()).isEqualTo(0);
    }

    @Test
    void shouldReconstituteFromExistingEntity() {
        SysUser entity = new SysUser();
        entity.setId("u-001");
        entity.setUsername("test");

        UserAggregate user = UserAggregate.from(entity);

        assertThat(user.getId()).isEqualTo("u-001");
        assertThat(user.getUsername()).isEqualTo("test");
    }

    @Test
    void shouldUpdateProfileWithNonNullFieldsOnly() {
        UserAggregate user = UserAggregate.create("admin", "pwd");
        user.updateProfile("Nick", null, "a@b.com", null, null, 1, null);

        assertThat(user.getNickname()).isEqualTo("Nick");
        assertThat(user.getEmail()).isEqualTo("a@b.com");
        assertThat(user.getGender()).isEqualTo(1);
        assertThat(user.getRealName()).isNull();
        assertThat(user.getPhone()).isNull();
    }

    @Test
    void shouldNotOverwriteWithNullOnProfileUpdate() {
        UserAggregate user = UserAggregate.create("admin", "pwd");
        user.updateProfile("Nick", "Real", "a@b.com", "123", "avatar.png", 1, "note");
        user.updateProfile(null, null, "new@b.com", null, null, null, null);

        assertThat(user.getNickname()).isEqualTo("Nick");
        assertThat(user.getEmail()).isEqualTo("new@b.com");
        assertThat(user.getPhone()).isEqualTo("123");
    }

    @Test
    void shouldSetPasswordUpdateTimeOnPasswordChange() {
        UserAggregate user = UserAggregate.create("admin", "old_pwd");

        user.updatePassword("new_encoded_pwd");

        assertThat(user.getPassword()).isEqualTo("new_encoded_pwd");
        assertThat(user.toEntity().getPwdUpdateTime()).isNotNull();
    }

    @Test
    void shouldResetAttemptsAndLockOnLoginSuccess() {
        UserAggregate user = UserAggregate.create("admin", "pwd");
        user.toEntity().setLoginAttempts(3);
        user.toEntity().setLockTime(java.time.LocalDateTime.now());

        user.recordLoginSuccess("192.168.1.1");

        assertThat(user.toEntity().getLoginAttempts()).isEqualTo(0);
        assertThat(user.toEntity().getLockTime()).isNull();
        assertThat(user.toEntity().getLoginIp()).isEqualTo("192.168.1.1");
        assertThat(user.toEntity().getLoginTime()).isNotNull();
    }

    @Test
    void shouldIncrementAttemptsOnLoginFailure() {
        UserAggregate user = UserAggregate.create("admin", "pwd");

        user.recordLoginFailure(5);
        user.recordLoginFailure(5);

        assertThat(user.toEntity().getLoginAttempts()).isEqualTo(2);
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    void shouldLockAccountWhenMaxAttemptsReached() {
        UserAggregate user = UserAggregate.create("admin", "pwd");

        user.recordLoginFailure(3);
        user.recordLoginFailure(3);
        user.recordLoginFailure(3);

        assertThat(user.toEntity().getLoginAttempts()).isEqualTo(3);
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void shouldHandleNullLoginAttemptsGracefully() {
        UserAggregate user = UserAggregate.create("admin", "pwd");
        user.toEntity().setLoginAttempts(null);

        user.recordLoginFailure(5);

        assertThat(user.toEntity().getLoginAttempts()).isEqualTo(1);
    }

    @Test
    void shouldDisableUser() {
        UserAggregate user = UserAggregate.create("admin", "pwd");

        user.disable();

        assertThat(user.isDisabled()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED.getCode());
    }

    @Test
    void shouldEnableUserAndResetAttempts() {
        UserAggregate user = UserAggregate.create("admin", "pwd");
        user.disable();
        user.toEntity().setLoginAttempts(3);
        user.toEntity().setLockTime(java.time.LocalDateTime.now());

        user.enable();

        assertThat(user.isDisabled()).isFalse();
        assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL.getCode());
        assertThat(user.toEntity().getLoginAttempts()).isEqualTo(0);
        assertThat(user.toEntity().getLockTime()).isNull();
    }

    @Test
    void shouldReturnCorrectDisabledStatus() {
        UserAggregate user = UserAggregate.create("admin", "pwd");

        assertThat(user.isDisabled()).isFalse();

        user.disable();
        assertThat(user.isDisabled()).isTrue();
    }

    @Test
    void shouldReturnCorrectLockedStatus() {
        UserAggregate user = UserAggregate.create("admin", "pwd");

        assertThat(user.isLocked()).isFalse();

        user.toEntity().setLockTime(java.time.LocalDateTime.now());
        assertThat(user.isLocked()).isTrue();
    }
}
