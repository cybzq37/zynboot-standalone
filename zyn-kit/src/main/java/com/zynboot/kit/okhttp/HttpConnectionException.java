package com.zynboot.kit.okhttp;

/**
 * HTTP 连接异常（无法建立连接、DNS 解析失败等）。
 */
public class HttpConnectionException extends HttpClientException {

    private final String url;

    public HttpConnectionException(String url, Throwable cause) {
        super("HTTP connection failed: " + url, cause);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
