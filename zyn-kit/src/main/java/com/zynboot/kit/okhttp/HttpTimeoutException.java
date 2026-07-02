package com.zynboot.kit.okhttp;

/**
 * HTTP 超时异常（连接超时或读取超时）。
 */
public class HttpTimeoutException extends HttpClientException {

    private final String url;

    public HttpTimeoutException(String url, Throwable cause) {
        super("HTTP request timed out: " + url, cause);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
