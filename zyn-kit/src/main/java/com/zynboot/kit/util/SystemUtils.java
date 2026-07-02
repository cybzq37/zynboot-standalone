package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 系统信息工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SystemUtils {

    // ==================== OS ====================

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    public static String getOsArch() {
        return System.getProperty("os.arch");
    }

    public static String getOsVersion() {
        return System.getProperty("os.version");
    }

    public static boolean isWindows() {
        return getOsName().toLowerCase().contains("win");
    }

    public static boolean isLinux() {
        return getOsName().toLowerCase().contains("linux");
    }

    public static boolean isMac() {
        return getOsName().toLowerCase().contains("mac");
    }

    // ==================== JVM ====================

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public static String getJavaVendor() {
        return System.getProperty("java.vendor");
    }

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * JVM 最大内存（MB）。
     */
    public static long getMaxMemoryMB() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    /**
     * JVM 已用内存（MB）。
     */
    public static long getUsedMemoryMB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    /**
     * JVM 空闲内存（MB）。
     */
    public static long getFreeMemoryMB() {
        return Runtime.getRuntime().freeMemory() / (1024 * 1024);
    }

    /**
     * JVM 启动时间（毫秒时间戳）。
     */
    public static long getJvmStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    /**
     * JVM 运行时长（秒）。
     */
    public static long getJvmUptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }

    // ==================== 磁盘 ====================

    /**
     * 获取指定路径所在磁盘的可用空间（GB）。
     */
    public static long getAvailableDiskGB(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            return store.getUsableSpace() / (1024 * 1024 * 1024);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取指定路径所在磁盘的总空间（GB）。
     */
    public static long getTotalDiskGB(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            return store.getTotalSpace() / (1024 * 1024 * 1024);
        } catch (Exception e) {
            return -1;
        }
    }

    // ==================== 用户 ====================

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    // ==================== 环境变量 ====================

    /**
     * 获取环境变量，不存在返回默认值。
     */
    public static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取系统属性，不存在返回默认值。
     */
    public static String getProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }
}
