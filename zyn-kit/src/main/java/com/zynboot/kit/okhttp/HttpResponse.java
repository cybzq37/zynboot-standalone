package com.zynboot.kit.okhttp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP 响应封装。
 */
public class HttpResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;
    private final byte[] bodyBytes;
    private final String errorMessage;

    private HttpResponse(int statusCode, Map<String, String> headers, String body, byte[] bodyBytes, String errorMessage) {
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.body = body;
        this.bodyBytes = bodyBytes;
        this.errorMessage = errorMessage;
    }

    public static HttpResponse success(int statusCode, Map<String, String> headers, String body) {
        return new HttpResponse(statusCode, headers, body, null, null);
    }

    public static HttpResponse successBytes(int statusCode, Map<String, String> headers, byte[] bodyBytes) {
        return new HttpResponse(statusCode, headers, null, bodyBytes, null);
    }

    public static HttpResponse failure(String errorMessage) {
        return new HttpResponse(-1, Collections.emptyMap(), null, null, errorMessage);
    }

    public int statusCode() { return statusCode; }
    public Map<String, String> headers() { return headers; }
    public String body() { return body; }
    public byte[] bodyBytes() { return bodyBytes; }
    public String errorMessage() { return errorMessage; }

    public boolean isSuccessful() {
        return errorMessage == null && statusCode >= 200 && statusCode < 300;
    }

    /**
     * 获取指定响应头（忽略大小写）。
     */
    public String header(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 获取指定响应头的所有值（逗号分隔时拆分）。
     */
    public List<String> headerValues(String name) {
        String value = headers.get(name.toLowerCase(Locale.ROOT));
        if (value == null) return Collections.emptyList();
        return List.of(value.split(",\\s*"));
    }

    static Map<String, String> toHeaderMap(okhttp3.Headers okHeaders) {
        if (okHeaders == null || okHeaders.size() == 0) return Collections.emptyMap();
        Map<String, String> map = new LinkedHashMap<>(okHeaders.size());
        for (int i = 0; i < okHeaders.size(); i++) {
            map.put(okHeaders.name(i).toLowerCase(Locale.ROOT), okHeaders.value(i));
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        if (errorMessage != null) return "HttpResponse{error=" + errorMessage + "}";
        return "HttpResponse{status=" + statusCode + ", body=" + (body != null ? body.length() + " chars" : "null") + "}";
    }
}
