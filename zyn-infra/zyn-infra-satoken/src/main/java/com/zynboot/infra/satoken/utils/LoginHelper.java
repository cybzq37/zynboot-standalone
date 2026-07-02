package com.zynboot.infra.satoken.utils;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.zynboot.infra.satoken.config.SaTokenProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录鉴权助手
 * <p>
 * 支持多用户体系和多设备类型：
 * <ul>
 *   <li>userType - 用户类型（如：pc、app）</li>
 *   <li>device - 设备类型（如：web、ios）</li>
 * </ul>
 *
 * @author lichunqing
 */
@Slf4j
public class LoginHelper {

    private static final String LOGIN_USER_KEY = "loginUser";
    private static final String USER_ID_KEY = "userId";

    private static volatile SaTokenProperties properties;

    public static void setProperties(SaTokenProperties properties) {
        LoginHelper.properties = properties;
    }

    /**
     * 用户登录
     *
     * @param loginId   登录ID
     * @param userId    用户ID（UUID 字符串）
     * @param loginUser 登录用户对象
     */
    public static void login(Object loginId, String userId, Object loginUser) {
        loginByDevice(loginId, userId, loginUser, null);
    }

    /**
     * 用户登录（指定设备类型）
     *
     * @param loginId   登录ID
     * @param userId    用户ID（UUID 字符串）
     * @param loginUser 登录用户对象
     * @param device    设备类型
     */
    public static void loginByDevice(Object loginId, String userId, Object loginUser, String device) {
        if (loginId == null) {
            throw new IllegalArgumentException("loginId must not be null");
        }

        SaStorage storage = SaHolder.getStorage();
        storage.set(LOGIN_USER_KEY, loginUser);
        storage.set(USER_ID_KEY, userId);

        SaLoginModel model = new SaLoginModel();
        if (device != null && !device.isBlank()) {
            model.setDevice(device);
        }

        StpUtil.login(loginId, model.setExtra(USER_ID_KEY, userId));
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取当前登录用户（多级缓存：Request -> TokenSession）
     */
    @SuppressWarnings("unchecked")
    public static <T> T getLoginUser() {
        T loginUser = (T) SaHolder.getStorage().get(LOGIN_USER_KEY);
        if (loginUser != null) {
            return loginUser;
        }

        SaSession session = StpUtil.getTokenSession();
        if (session == null) {
            return null;
        }

        loginUser = (T) session.get(LOGIN_USER_KEY);
        SaHolder.getStorage().set(LOGIN_USER_KEY, loginUser);
        return loginUser;
    }

    /**
     * 根据 Token 获取登录用户
     */
    @SuppressWarnings("unchecked")
    public static <T> T getLoginUser(String token) {
        SaSession session = StpUtil.getTokenSessionByToken(token);
        return session == null ? null : (T) session.get(LOGIN_USER_KEY);
    }

    /**
     * 获取当前用户ID（UUID 字符串）
     */
    public static String getUserId() {
        Object userId = SaHolder.getStorage().get(USER_ID_KEY);
        if (userId == null) {
            try {
                userId = StpUtil.getExtra(USER_ID_KEY);
                SaHolder.getStorage().set(USER_ID_KEY, userId);
            } catch (Exception e) {
                log.warn("Failed to get userId from token extra", e);
                return null;
            }
        }
        return userId == null ? null : String.valueOf(userId);
    }

    /**
     * 判断是否为超级管理员
     */
    public static boolean isRoot(String userId) {
        return userId != null && properties != null
                && userId.equals(properties.getRootUserId());
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isRoot() {
        return isRoot(getUserId());
    }
}
