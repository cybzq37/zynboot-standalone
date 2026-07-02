package com.zynboot.kit.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<String> details
) {

    public ErrorResponse {
        path = path == null ? "" : path;
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ErrorResponse of(String code, String message, String path, List<String> details) {
        return new ErrorResponse(code, message, Instant.now(), path, details);
    }
}
