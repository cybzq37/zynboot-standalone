package com.zynboot.infra.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 配置属性
 *
 * @author lichunqing
 */
@Data
@ConfigurationProperties(prefix = "zyn.satoken")
public class SaTokenProperties {

    /**
     * 登录类型标识
     */
    private String loginType = "login";

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer";

    /**
     * 超级管理员用户ID（主键为 UUID 字符串，需与实际 root 用户 id 一致）
     */
    private String rootUserId = "1";

    /**
     * 是否注册全局登录拦截器（对所有请求默认执行 checkLogin，并处理 @SaCheckPermission 等注解）。
     */
    private boolean interceptorEnabled = true;

    /**
     * 免登录白名单路径（相对 context-path，支持 Ant 风格）。
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/api/v1/auth/login",
            "/error",
            "/favicon.ico",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/**"
    ));
}
