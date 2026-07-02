package com.zynboot.kit.exception;

import java.util.List;

public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    public static final String BAD_REQUEST_CODE = "400";
    public static final String INTERNAL_ERROR_CODE = "500";
    private static final String DEFAULT_BAD_REQUEST_MESSAGE = "Bad request";
    private static final String DEFAULT_INTERNAL_ERROR_MESSAGE = "Internal server error";

    private final String code;
    private final int statusCode;
    private final List<String> details;

    public BaseException(String message) {
        this(400, BAD_REQUEST_CODE, message);
    }

    public BaseException(String code, String message) {
        this(400, code, message);
    }

    public BaseException(String code, String message, Throwable cause) {
        this(400, code, message, cause);
    }

    public BaseException(int statusCode, String code, String message) {
        this(statusCode, code, message, null, List.of());
    }

    public BaseException(int statusCode, String code, String message, List<String> details) {
        this(statusCode, code, message, null, details);
    }

    public BaseException(int statusCode, String code, String message, Throwable cause) {
        this(statusCode, code, message, cause, List.of());
    }

    public BaseException(int statusCode, String code, String message, Throwable cause, List<String> details) {
        super(normalizeMessage(message, statusCode), cause);
        this.statusCode = statusCode <= 0 ? 400 : statusCode;
        this.code = normalizeCode(code, this.statusCode);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public static BaseException badRequest(String message) {
        return new BaseException(message);
    }

    public static BaseException badRequest(String code, String message) {
        return new BaseException(code, message);
    }

    public static BaseException badRequest(String code, String message, List<String> details) {
        return new BaseException(400, code, message, details);
    }

    public static BaseException internalError(String message, Throwable cause) {
        return new BaseException(500, INTERNAL_ERROR_CODE, message, cause);
    }

    public static BaseException internalError(String code, String message, Throwable cause) {
        return new BaseException(500, code, message, cause);
    }

    public static BaseException internalError(String code, String message, Throwable cause, List<String> details) {
        return new BaseException(500, code, message, cause, details);
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return statusCode;
    }

    public List<String> getDetails() {
        return details;
    }

    private static String normalizeCode(String code, int statusCode) {
        if (code != null && !code.isBlank()) {
            return code;
        }
        return statusCode >= 500 ? INTERNAL_ERROR_CODE : BAD_REQUEST_CODE;
    }

    private static String normalizeMessage(String message, int statusCode) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return statusCode >= 500
                ? DEFAULT_INTERNAL_ERROR_MESSAGE
                : DEFAULT_BAD_REQUEST_MESSAGE;
    }
}
