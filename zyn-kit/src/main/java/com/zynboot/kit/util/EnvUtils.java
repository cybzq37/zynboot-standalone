package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 运行环境判断工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnvUtils {

    /**
     * 获取当前 Spring profile（从系统属性或环境变量）。
     */
    public static String getActiveProfile() {
        String profile = System.getProperty("spring.profiles.active");
        if (profile != null) return profile;
        profile = System.getenv("SPRING_PROFILES_ACTIVE");
        return profile != null ? profile : "default";
    }

    /**
     * 判断是否为开发环境。
     */
    public static boolean isDev() {
        String profile = getActiveProfile();
        return profile.contains("dev") || profile.contains("local");
    }

    /**
     * 判断是否为测试环境。
     */
    public static boolean isTest() {
        return getActiveProfile().contains("test");
    }

    /**
     * 判断是否为生产环境。
     */
    public static boolean isProd() {
        String profile = getActiveProfile();
        return profile.contains("prod") || profile.contains("production");
    }

    /**
     * 判断是否运行在 Docker 容器内。
     */
    public static boolean isDocker() {
        return Files.exists(Path.of("/.dockerenv"));
    }

    /**
     * 判断是否运行在 Kubernetes Pod 内。
     */
    public static boolean isKubernetes() {
        return System.getenv("KUBERNETES_SERVICE_HOST") != null;
    }

    /**
     * 获取应用名称（spring.application.name）。
     */
    public static String getAppName() {
        String name = System.getProperty("spring.application.name");
        return name != null ? name : "unknown";
    }
}
