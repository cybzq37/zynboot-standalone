package com.zynboot.infra.web.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.exception.BaseException;
import com.zynboot.kit.exception.ErrorResponse;
import com.zynboot.kit.response.ApiResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ResponseProperties properties;
    private final ObjectMapper objectMapper;

    public GlobalResponseBodyAdvice(ResponseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (isSpringDocEndpoint(returnType)) {
            return false;
        }
        if (hasIgnoreResponseWrap(returnType)) {
            return false;
        }
        return !isPassthroughReturnType(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // String 返回值由 StringHttpMessageConverter 处理，需手动序列化为 JSON
        // 其他类型由 MappingJackson2HttpMessageConverter 处理，直接返回包装对象即可
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(wrap(body, request));
            } catch (JsonProcessingException e) {
                throw BaseException.internalError("Serialize wrapped string response failed", e);
            }
        }

        if (body instanceof ApiResponse<?> || body instanceof ErrorResponse) {
            return body;
        }
        if (body == null || body instanceof Resource || body instanceof byte[] || body instanceof StreamingResponseBody) {
            return body == null ? wrap(null, request) : body;
        }
        return wrap(body, request);
    }

    private ApiResponse<Object> wrap(Object body, ServerHttpRequest request) {
        return ApiResponse.success(
                properties.getSuccessCode(),
                properties.getSuccessMessage(),
                extractPath(request),
                body
        );
    }

    private String extractPath(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getRequestURI();
        }
        return request.getURI() == null ? "" : request.getURI().getPath();
    }

    private boolean hasIgnoreResponseWrap(MethodParameter returnType) {
        if (returnType.getMethod() != null
                && AnnotatedElementUtils.hasAnnotation(returnType.getMethod(), IgnoreResponseWrap.class)) {
            return true;
        }
        return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), IgnoreResponseWrap.class);
    }

    private boolean isSpringDocEndpoint(MethodParameter returnType) {
        Class<?> containingClass = returnType.getContainingClass();
        Package endpointPackage = containingClass.getPackage();
        if (endpointPackage == null) {
            return false;
        }
        return endpointPackage.getName().startsWith("org.springdoc");
    }

    private boolean isPassthroughReturnType(Class<?> returnType) {
        return HttpEntity.class.isAssignableFrom(returnType)
                || Resource.class.isAssignableFrom(returnType)
                || byte[].class.equals(returnType)
                || StreamingResponseBody.class.isAssignableFrom(returnType);
    }
}
