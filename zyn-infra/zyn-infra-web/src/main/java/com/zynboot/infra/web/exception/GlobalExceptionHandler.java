package com.zynboot.infra.web.exception;

import com.zynboot.kit.exception.BaseException;
import com.zynboot.kit.exception.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String MALFORMED_REQUEST_MESSAGE = "Malformed request body";
    private static final String INVALID_REQUEST_PARAMETER_MESSAGE = "Invalid request parameter";
    private static final int MAX_STACK_TRACE_LINES = 20;

    private final GlobalExceptionProperties properties;

    public GlobalExceptionHandler(GlobalExceptionProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, WebRequest request) {
        if (ex.getStatus() >= 500) {
            log.error("Application exception, code={}, msg={}", ex.getCode(), ex.getMessage(), ex);
        } else if (properties.isLogWarnForBusiness()) {
            log.warn("Business exception, code={}, msg={}", ex.getCode(), ex.getMessage());
        }
        HttpStatusCode status = HttpStatus.valueOf(ex.getStatus());
        return build(status, ex.getCode(), ex.getMessage(), request, resolveDetails(ex.getDetails(), ex, status));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, WebRequest request) {
        String message;
        if (ex instanceof HttpMessageNotReadableException) {
            message = MALFORMED_REQUEST_MESSAGE;
        } else if (ex instanceof IllegalArgumentException
                || ex instanceof MethodArgumentTypeMismatchException
                || ex instanceof MissingServletRequestParameterException) {
            log.warn("Bad request: {}", ex.getMessage());
            message = INVALID_REQUEST_PARAMETER_MESSAGE;
        } else {
            message = ex.getMessage();
        }
        return build(HttpStatus.BAD_REQUEST, BaseException.BAD_REQUEST_CODE, message, request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, BaseException.BAD_REQUEST_CODE, VALIDATION_FAILED_MESSAGE, request, collectErrorDetails(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, BaseException.BAD_REQUEST_CODE, VALIDATION_FAILED_MESSAGE, request, collectErrorDetails(ex.getBindingResult()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex, WebRequest request) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, BaseException.BAD_REQUEST_CODE, VALIDATION_FAILED_MESSAGE, request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(this::formatViolation)
                .toList();
        return build(HttpStatus.BAD_REQUEST, BaseException.BAD_REQUEST_CODE, VALIDATION_FAILED_MESSAGE, request, details);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, BaseException.BAD_REQUEST_CODE, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        String reason = ex.getReason();
        String message = reason != null ? reason : HttpStatus.valueOf(status.value()).getReasonPhrase();
        if (status.is5xxServerError()) {
            log.error("ResponseStatusException: {}", message, ex);
        } else {
            log.warn("ResponseStatusException: {}", message);
        }
        return build(status, BaseException.BAD_REQUEST_CODE, message, request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        if (properties.isIncludeStackTrace()) {
            log.error("Unhandled exception", ex);
        } else {
            log.error("Unhandled exception: {}", ex.toString());
        }
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                BaseException.INTERNAL_ERROR_CODE,
                INTERNAL_SERVER_ERROR_MESSAGE,
                request,
                resolveDetails(List.of(), ex, HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String code, String message, WebRequest request, List<String> details) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(code, message, path, details);
        return ResponseEntity.status(status).body(body);
    }

    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        String desc = request.getDescription(false);
        if (desc == null) {
            return "";
        }
        return desc.startsWith("uri=") ? desc.substring(4) : desc;
    }

    private List<String> collectErrorDetails(Errors errors) {
        return errors.getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + error.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private List<String> resolveDetails(List<String> details, Throwable ex, HttpStatusCode status) {
        if (!details.isEmpty()) {
            return details;
        }
        if (!properties.isIncludeStackTrace() || !status.is5xxServerError()) {
            return List.of();
        }
        Throwable target = ex.getCause() != null ? ex.getCause() : ex;
        return stackTraceLines(target);
    }

    private List<String> stackTraceLines(Throwable ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString().lines()
                .limit(MAX_STACK_TRACE_LINES)
                .toList();
    }
}
