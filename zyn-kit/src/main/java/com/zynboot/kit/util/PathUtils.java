package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * 路径工具类，处理 Windows / Linux 路径差异。
 * <p>
 * 所有路径字符串统一使用正斜杠 {@code /} 处理，
 * 仅在最终输出到操作系统时转为系统分隔符。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathUtils {

    private static final char SEP = File.separatorChar;

    // ==================== 标准化 ====================

    /**
     * 路径标准化（统一分隔符为当前系统风格，去除冗余分隔符）。
     * <p>
     * Windows: {@code normalize("a/b/c")} → {@code "a\\b\\c"}
     * Linux:   {@code normalize("a\\b\\c")} → {@code "a/b/c"}
     */
    public static String normalize(String path) {
        if (StringUtils.isBlank(path)) return path;
        String normalized = path.replace("\\", "/").replace("/", String.valueOf(SEP));
        normalized = normalized.replaceAll(String.valueOf(SEP) + "+", String.valueOf(SEP));
        if (normalized.length() > 1 && normalized.endsWith(String.valueOf(SEP))) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 路径标准化为 Linux 风格（正斜杠），适合跨平台存储和传输。
     */
    public static String toLinux(String path) {
        if (StringUtils.isBlank(path)) return path;
        return path.replace("\\", "/");
    }

    /**
     * 路径标准化为 Windows 风格（反斜杠）。
     */
    public static String toWindows(String path) {
        if (StringUtils.isBlank(path)) return path;
        return path.replace("/", "\\");
    }

    /**
     * 转为当前操作系统的路径风格。
     */
    public static String toNative(String path) {
        if (StringUtils.isBlank(path)) return path;
        return path.replace("\\", "/").replace("/", String.valueOf(SEP));
    }

    // ==================== 拼接 ====================

    /**
     * 拼接路径（自动处理分隔符，去除冗余）。
     * <p>
     * {@code join("a/b", "c/d")} → {@code "a/b/c/d"}
     * {@code join("a/b/", "/c/d")} → {@code "a/b/c/d"}
     */
    public static String join(String... parts) {
        if (parts == null || parts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.isBlank(part)) continue;
            String normalized = toLinux(part);
            if (sb.length() > 0 && !normalized.startsWith("/")) sb.append("/");
            sb.append(normalized);
        }
        String result = sb.toString().replaceAll("/+", "/");
        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    // ==================== 提取 ====================

    /**
     * 获取父目录路径。
     * <p>
     * {@code getParent("/a/b/c")} → {@code "/a/b"}
     */
    public static String getParent(String path) {
        if (StringUtils.isBlank(path)) return null;
        String normalized = toLinux(path);
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash <= 0) return "/";
        return normalized.substring(0, lastSlash);
    }

    /**
     * 获取路径最后一段（文件名或目录名）。
     * <p>
     * {@code getFileName("/a/b/c.txt")} → {@code "c.txt"}
     */
    public static String getFileName(String path) {
        if (StringUtils.isBlank(path)) return "";
        String normalized = toLinux(path);
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    /**
     * 获取文件扩展名（不含点号）。
     * <p>
     * {@code getExtension("/a/b/c.txt")} → {@code "txt"}
     */
    public static String getExtension(String path) {
        String fileName = getFileName(path);
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : "";
    }

    /**
     * 获取文件名（不含扩展名）。
     * <p>
     * {@code getBaseName("/a/b/c.txt")} → {@code "c"}
     */
    public static String getBaseName(String path) {
        String fileName = getFileName(path);
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // ==================== 判断 ====================

    /**
     * 判断路径是否为绝对路径。
     * <p>
     * Unix: {@code /a/b} → true
     * Windows: {@code C:\a\b} → true
     */
    public static boolean isAbsolute(String path) {
        if (StringUtils.isBlank(path)) return false;
        if (path.startsWith("/")) return true;
        return path.matches("^[A-Za-z]:\\\\.*");
    }

    /**
     * 判断 child 是否在 parent 目录下（防止路径穿越攻击）。
     * <p>
     * {@code isSubPath("/a/b", "/a/b/c")} → true
     * {@code isSubPath("/a/b", "/a/b/../c")} → false
     */
    public static boolean isSubPath(String parent, String child) {
        if (StringUtils.isBlank(parent) || StringUtils.isBlank(child)) return false;
        String np = toLinux(parent);
        String nc = toLinux(child);
        // 先标准化再比较，防止 ../ 穿越
        String normalizedParent = normalizePath(np);
        String normalizedChild = normalizePath(nc);
        return normalizedChild.startsWith(normalizedParent + "/")
                || normalizedChild.equals(normalizedParent);
    }

    // ==================== 转换 ====================

    /**
     * 计算相对路径（从 base 到 target）。
     * <p>
     * {@code relativize("/a/b/c", "/a/b/d")} → {@code "../d"}
     */
    public static String relativize(String base, String target) {
        if (StringUtils.isBlank(base) || StringUtils.isBlank(target)) return target;
        String[] baseParts = toLinux(base).split("/");
        String[] targetParts = toLinux(target).split("/");
        int common = 0;
        while (common < baseParts.length && common < targetParts.length
                && baseParts[common].equals(targetParts[common])) {
            common++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = common; i < baseParts.length; i++) {
            if (!baseParts[i].isEmpty()) sb.append("../");
        }
        for (int i = common; i < targetParts.length; i++) {
            if (!targetParts[i].isEmpty()) {
                if (sb.length() > 0 && !sb.toString().endsWith("/")) sb.append("/");
                sb.append(targetParts[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : ".";
    }

    /**
     * 清理路径（去除冗余分隔符、解析 . 和 ..）。
     * <p>
     * {@code cleanPath("/a/b/../c/./d")} → {@code "/a/c/d"}
     */
    public static String cleanPath(String path) {
        if (StringUtils.isBlank(path)) return path;
        return normalizePath(toLinux(path));
    }

    // ==================== 内部 ====================

    /**
     * 解析 . 和 .. ，标准化路径。
     */
    private static String normalizePath(String path) {
        if (StringUtils.isBlank(path)) return path;
        String[] parts = path.split("/");
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        for (String part : parts) {
            if ("..".equals(part)) {
                if (!stack.isEmpty() && !"..".equals(stack.peek())) {
                    stack.pop();
                }
            } else if (!".".equals(part) && !part.isEmpty()) {
                stack.push(part);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (path.startsWith("/")) sb.append("/");
        java.util.ArrayList<String> list = new java.util.ArrayList<>(stack);
        java.util.Collections.reverse(list);
        sb.append(String.join("/", list));
        return sb.length() > 0 ? sb.toString() : "/";
    }
}
