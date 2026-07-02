package com.zynboot.kit.okhttp;

/**
 * HTTP 服务端错误异常（4xx/5xx 状态码）。
 */
public class HttpServerException extends HttpClientException {

    private final int statusCode;
    private final String responseBody;

    public HttpServerException(int statusCode, String responseBody) {
        super("HTTP server error, status=" + statusCode + ", body=" + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
