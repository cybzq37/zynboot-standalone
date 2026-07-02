package com.zynboot.infra.web.exception;

import com.zynboot.kit.exception.BaseException;
import com.zynboot.kit.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new GlobalExceptionProperties());

    @Test
    void shouldRespectBaseExceptionStatusAndDetails() {
        BaseException exception = new BaseException(
                422,
                "BIZ-422",
                "Rule violated",
                null,
                List.of("name: duplicated")
        );

        ResponseEntity<ErrorResponse> response = handler.handleBaseException(exception, request("/users"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("BIZ-422");
        assertThat(response.getBody().path()).isEqualTo("/users");
        assertThat(response.getBody().details()).containsExactly("name: duplicated");
    }

    @Test
    void shouldReturnInternalServerErrorForUnhandledException() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new IllegalStateException("boom"), request("/jobs"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(BaseException.INTERNAL_ERROR_CODE);
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
        assertThat(response.getBody().path()).isEqualTo("/jobs");
    }

    @Test
    void shouldUseStableMessageForUnreadableRequestBody() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0])),
                request("/users")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed request body");
    }

    @Test
    void shouldIncludeStackTraceDetailsWhenConfigured() {
        GlobalExceptionProperties properties = new GlobalExceptionProperties();
        properties.setIncludeStackTrace(true);
        GlobalExceptionHandler stackTraceHandler = new GlobalExceptionHandler(properties);

        ResponseEntity<ErrorResponse> response = stackTraceHandler.handleException(new IllegalStateException("boom"), request("/jobs"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).isNotEmpty();
        assertThat(response.getBody().details().get(0)).contains("IllegalStateException");
    }

    private static ServletWebRequest request(String path) {
        return new ServletWebRequest(new MockHttpServletRequest("GET", path));
    }
}
