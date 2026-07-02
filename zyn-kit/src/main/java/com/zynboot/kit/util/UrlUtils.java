package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * URL 工具类。
 * <p>
 * 支持编解码、解析、拼接、参数处理等。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlUtils {

    private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

    // ==================== 编解码 ====================

    /**
     * URL 编码（UTF-8）。
     */
    public static String encode(String value) {
        if (StringUtils.isBlank(value)) return value;
        try {
            return URLEncoder.encode(value, DEFAULT_ENCODING);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * URL 解码（UTF-8）。
     */
    public static String decode(String value) {
        if (StringUtils.isBlank(value)) return value;
        try {
            return URLDecoder.decode(value, DEFAULT_ENCODING);
        } catch (Exception e) {
            return value;
        }
    }

    // ==================== 解析 ====================

    /**
     * 解析 URL 为 URI 对象，失败返回 null。
     */
    public static URI parseUri(String url) {
        if (StringUtils.isBlank(url)) return null;
        try {
            return URI.create(url);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 scheme（协议）。
     */
    public static String getScheme(String url) {
        URI uri = parseUri(url);
        return uri != null ? uri.getScheme() : null;
    }

    /**
     * 获取 host（主机名）。
     */
    public static String getHost(String url) {
        URI uri = parseUri(url);
        return uri != null ? uri.getHost() : null;
    }

    /**
     * 获取 port（端口），未指定返回 -1。
     */
    public static int getPort(String url) {
        URI uri = parseUri(url);
        return uri != null ? uri.getPort() : -1;
    }

    /**
     * 获取 path（路径）。
     */
    public static String getPath(String url) {
        URI uri = parseUri(url);
        return uri != null ? uri.getPath() : null;
    }

    /**
     * 获取 query（查询字符串）。
     */
    public static String getQuery(String url) {
        URI uri = parseUri(url);
        return uri != null ? uri.getQuery() : null;
    }

    /**
     * 获取 fragment（锚点）。
     */
    public static String getFragment(String url) {
        URI uri = parseUri(url);
        return uri != null ? uri.getFragment() : null;
    }

    /**
     * 获取 base URL（scheme + host + port，不含路径）。
     */
    public static String getBaseUrl(String url) {
        URI uri = parseUri(url);
        if (uri == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > 0) sb.append(":").append(uri.getPort());
        return sb.toString();
    }

    // ==================== 查询参数 ====================

    /**
     * 解析查询字符串为 Map。
     * <p>
     * {@code parseQuery("name=test&page=1")} → {@code {name=test, page=1}}
     */
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (StringUtils.isBlank(query)) return params;
        // 去掉开头的 ?
        if (query.startsWith("?")) query = query.substring(1);
        for (String pair : query.split("&")) {
            if (StringUtils.isBlank(pair)) continue;
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            } else {
                params.put(decode(pair), "");
            }
        }
        return params;
    }

    /**
     * 从 URL 中解析查询参数。
     */
    public static Map<String, String> parseQueryFromUrl(String url) {
        String query = getQuery(url);
        return parseQuery(query);
    }

    /**
     * Map 转查询字符串。
     * <p>
     * {@code buildQuery(Map.of("name", "test", "page", "1"))} → {@code "name=test&page=1"}
     */
    public static String buildQuery(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(encode(entry.getKey()));
            if (entry.getValue() != null) {
                sb.append("=").append(encode(entry.getValue()));
            }
        }
        return sb.toString();
    }

    /**
     * 给 URL 追加查询参数。
     */
    public static String appendQuery(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) return url;
        String query = buildQuery(params);
        if (StringUtils.isBlank(url)) return query;
        return url.contains("?") ? url + "&" + query : url + "?" + query;
    }

    /**
     * 给 URL 追加单个查询参数。
     */
    public static String appendQuery(String url, String key, String value) {
        return appendQuery(url, Map.of(key, value));
    }

    /**
     * 移除 URL 中的查询参数。
     */
    public static String removeQuery(String url) {
        if (StringUtils.isBlank(url)) return url;
        int idx = url.indexOf('?');
        return idx > 0 ? url.substring(0, idx) : url;
    }

    /**
     * 移除 URL 中指定的查询参数。
     */
    public static String removeQueryParams(String url, String... keys) {
        if (StringUtils.isBlank(url) || keys == null || keys.length == 0) return url;
        Map<String, String> params = parseQueryFromUrl(url);
        for (String key : keys) params.remove(key);
        String base = removeQuery(url);
        return appendQuery(base, params);
    }

    // ==================== URL 拼接 ====================

    /**
     * 拼接 base URL 和路径。
     * <p>
     * {@code join("http://api.com/v1", "/users")} → {@code "http://api.com/v1/users"}
     */
    public static String join(String base, String path) {
        if (StringUtils.isBlank(base)) return path;
        if (StringUtils.isBlank(path)) return base;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (path.startsWith("/")) path = path.substring(1);
        return base + "/" + path;
    }

    // ==================== 判断 ====================

    /**
     * 判断是否为合法 HTTP/HTTPS URL。
     */
    public static boolean isHttpUrl(String url) {
        if (StringUtils.isBlank(url)) return false;
        String scheme = getScheme(url);
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    /**
     * 判断是否为 HTTPS。
     */
    public static boolean isHttps(String url) {
        return "https".equalsIgnoreCase(getScheme(url));
    }

    /**
     * 判断是否为 localhost / 127.0.0.1 / 0.0.0.0。
     */
    public static boolean isLocalhost(String url) {
        String host = getHost(url);
        if (host == null) return false;
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "0.0.0.0".equals(host);
    }

    /**
     * 获取域名（去掉子域名前缀）。
     * <p>
     * {@code getDomain("https://api.example.com/path")} → {@code "example.com"}
     */
    public static String getDomain(String url) {
        String host = getHost(url);
        if (host == null) return null;
        // IP 地址直接返回
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return host;
        // 提取顶级域名 + 一级域名
        String[] parts = host.split("\\.");
        if (parts.length <= 2) return host;
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    /**
     * 清理 URL（去除多余空格、尾部斜杠）。
     */
    public static String clean(String url) {
        if (StringUtils.isBlank(url)) return url;
        url = url.trim();
        // 去掉末尾斜杠（保留协议后的 //）
        while (url.length() > 1 && url.endsWith("/") && !url.endsWith("://")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
