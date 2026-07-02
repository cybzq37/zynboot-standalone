package com.zynboot.kit.response;

import java.time.Instant;

public final class ApiResponse<T> {

    public static final String DEFAULT_SUCCESS_CODE = "0";
    public static final String DEFAULT_FAIL_CODE = "9999";
    public static final String DEFAULT_SUCCESS_MESSAGE = "OK";

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final String path;
    private final T data;

    private ApiResponse(String code, String message, Instant timestamp, String path, T data) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path == null ? "" : path;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String code, String message, String path, T data) {
        return new ApiResponse<>(
                normalizeCode(code),
                normalizeMessage(message),
                Instant.now(),
                path,
                data
        );
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(null, null, null, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(DEFAULT_FAIL_CODE, message, Instant.now(), null, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public T getData() {
        return data;
    }

    private static String normalizeCode(String code) {
        return code == null || code.isBlank() ? DEFAULT_SUCCESS_CODE : code;
    }

    private static String normalizeMessage(String message) {
        return message == null || message.isBlank() ? DEFAULT_SUCCESS_MESSAGE : message;
    }
}
