package com.zynboot.infra.web.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.exception.BaseException;
import com.zynboot.kit.exception.ErrorResponse;
import com.zynboot.kit.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalResponseBodyAdviceTest {

    private final ResponseProperties properties = new ResponseProperties();
    private final GlobalResponseBodyAdvice advice = new GlobalResponseBodyAdvice(properties, new ObjectMapper());

    @Test
    void shouldRaiseBaseExceptionWhenStringWrappingSerializationFails() throws NoSuchMethodException {
        GlobalResponseBodyAdvice advice = new GlobalResponseBodyAdvice(properties, failingObjectMapper());
        MethodParameter returnType = new MethodParameter(TestController.class.getDeclaredMethod("text"), -1);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

        assertThatThrownBy(() -> advice.beforeBodyWrite(
                "payload",
                returnType,
                MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class,
                new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/demo")),
                response
        ))
                .isInstanceOf(BaseException.class)
                .satisfies(throwable -> {
                    BaseException exception = (BaseException) throwable;
                    assertThat(exception.getStatus()).isEqualTo(500);
                    assertThat(exception.getCode()).isEqualTo(BaseException.INTERNAL_ERROR_CODE);
                });

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void shouldNotWrapResponseEntityReturnType() throws NoSuchMethodException {
        MethodParameter returnType = new MethodParameter(TestController.class.getDeclaredMethod("entity"), -1);

        boolean supports = advice.supports(returnType, StringHttpMessageConverter.class);

        assertThat(supports).isFalse();
    }

    @Test
    void shouldKeepExistingWrappedBodiesUntouched() throws NoSuchMethodException {
        MethodParameter returnType = new MethodParameter(TestController.class.getDeclaredMethod("text"), -1);
        ApiResponse<String> apiResponse = ApiResponse.success("0", "OK", "/demo", "payload");
        ErrorResponse errorResponse = ErrorResponse.of("400", "Bad request", "/demo", null);

        Object wrappedApi = advice.beforeBodyWrite(
                apiResponse,
                returnType,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/demo")),
                new ServletServerHttpResponse(new MockHttpServletResponse())
        );
        Object wrappedError = advice.beforeBodyWrite(
                errorResponse,
                returnType,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/demo")),
                new ServletServerHttpResponse(new MockHttpServletResponse())
        );

        assertThat(wrappedApi).isSameAs(apiResponse);
        assertThat(wrappedError).isSameAs(errorResponse);
    }

    @Test
    void shouldFallbackToDefaultSuccessValuesWhenPropertiesBlank() {
        properties.setSuccessCode(" ");
        properties.setSuccessMessage("");

        ApiResponse<Object> response = ApiResponse.success(properties.getSuccessCode(), properties.getSuccessMessage(), "/demo", null);

        assertThat(response.getCode()).isEqualTo(ApiResponse.DEFAULT_SUCCESS_CODE);
        assertThat(response.getMessage()).isEqualTo(ApiResponse.DEFAULT_SUCCESS_MESSAGE);
    }

    private static ObjectMapper failingObjectMapper() {
        return new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                };
            }
        };
    }

    static class TestController {

        public String text() {
            return "ok";
        }

        public ResponseEntity<String> entity() {
            return ResponseEntity.status(HttpStatus.CREATED).body("ok");
        }
    }
}
