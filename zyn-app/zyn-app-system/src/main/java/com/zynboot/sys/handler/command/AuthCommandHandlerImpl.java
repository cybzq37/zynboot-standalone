package com.zynboot.sys.handler.command;

import com.zynboot.sys.response.user.LoginRes;
import com.zynboot.sys.response.user.LoginUserRes;
import com.zynboot.sys.response.user.UserRes;
import com.zynboot.sys.response.user.UserInfoRes;
import com.zynboot.sys.response.permission.MenuTreeRes;
import com.zynboot.infra.redis.RedisClient;
import com.zynboot.infra.satoken.utils.LoginHelper;
import com.zynboot.kit.exception.BaseException;
import com.zynboot.kit.util.BeanUtils;
import com.zynboot.sys.domain.aggregate.UserAggregate;
import com.zynboot.sys.domain.repository.UserRepository;
import com.zynboot.sys.handler.query.PermissionQueryHandler;
import com.zynboot.sys.handler.query.UserQueryHandler;
import com.zynboot.sys.infrastructure.entity.SysPermission;
import com.zynboot.sys.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证命令处理器（写操作）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandHandlerImpl implements AuthCommandHandler {

    /** 登录失败计数键。 */
    private static final String LOGIN_FAIL_KEY = "sys:login:fail:%s";
    /** 触发锁定的失败次数阈值。 */
    private static final int MAX_FAIL_ATTEMPTS = 5;
    /** 失败计数窗口 / 锁定时长。 */
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final UserQueryHandler userQueryHandler;
    private final PermissionQueryHandler permissionQueryHandler;
    private final RedisClient redisClient;

    @Override
    public LoginRes login(String username, String password) {
        if (isRateLimited(username)) {
            throw BaseException.badRequest("登录失败次数过多，请稍后再试");
        }

        UserAggregate user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    recordLoginFail(username);
                    return BaseException.badRequest("用户名或密码错误");
                });

        if (!PasswordUtils.matches(password, user.getPassword())) {
            recordLoginFail(username);
            throw BaseException.badRequest("用户名或密码错误");
        }
        if (user.isDisabled()) {
            throw BaseException.badRequest("账号已被停用");
        }
        if (user.isLocked()) {
            throw BaseException.badRequest("账号已被锁定");
        }

        clearLoginFail(username);
        user.recordLoginSuccess(null);
        userRepository.update(user);

        LoginUserRes loginUser = userQueryHandler.getLoginUser(user.getId());
        LoginHelper.login(user.getId(), user.getId(), loginUser);

        UserRes userRes = BeanUtils.copy(user, UserRes.class);
        String token = cn.dev33.satoken.stp.StpUtil.getTokenValue();
        return LoginRes.builder().token(token).userInfo(userRes).build();
    }

    @Override
    public UserInfoRes getCurrentUserInfo() {
        LoginUserRes loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw BaseException.badRequest("未登录");
        }

        UserRes userRes = BeanUtils.copy(
                userRepository.findById(loginUser.getUserId())
                        .map(u -> (Object) u)
                        .orElse(null),
                UserRes.class);

        List<SysPermission> menus = permissionQueryHandler.getPermsByUserId(loginUser.getUserId());
        List<MenuTreeRes> menuTree = menus.stream()
                .filter(p -> p.getPermType() <= 2)
                .map(p -> MenuTreeRes.builder()
                        .id(p.getId())
                        .parentId(p.getParentId())
                        .permName(p.getPermName())
                        .permType(p.getPermType())
                        .path(p.getPath())
                        .sort(p.getSort())
                        .visible(p.getVisible())
                        .build())
                .collect(Collectors.toList());

        return UserInfoRes.builder()
                .user(userRes)
                .roles(loginUser.getRoleCodes().stream().sorted().collect(Collectors.toList()))
                .permissions(loginUser.getPermCodes().stream().sorted().collect(Collectors.toList()))
                .menus(menuTree)
                .build();
    }

    /** 是否已达到失败上限（Redis 不可用时 fail-open，不阻断登录）。 */
    private boolean isRateLimited(String username) {
        try {
            return redisClient.get(LOGIN_FAIL_KEY.formatted(username))
                    .map(Long::parseLong)
                    .filter(count -> count >= MAX_FAIL_ATTEMPTS)
                    .isPresent();
        } catch (Exception e) {
            log.debug("Login rate-limit check skipped: {}", e.getMessage());
            return false;
        }
    }

    /** 记录一次登录失败，首次失败时设置窗口过期时间。 */
    private void recordLoginFail(String username) {
        try {
            String key = LOGIN_FAIL_KEY.formatted(username);
            Long count = redisClient.increment(key);
            if (count != null && count == 1L) {
                redisClient.expire(key, FAIL_WINDOW);
            }
        } catch (Exception e) {
            log.debug("Login fail recording skipped: {}", e.getMessage());
        }
    }

    /** 登录成功后清除失败计数。 */
    private void clearLoginFail(String username) {
        try {
            redisClient.delete(LOGIN_FAIL_KEY.formatted(username));
        } catch (Exception e) {
            log.debug("Login fail clearing skipped: {}", e.getMessage());
        }
    }
}
