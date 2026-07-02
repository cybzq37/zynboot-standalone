package com.zynboot.infra.web.version;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * 自动为标注了 {@link ApiVersion} 的路径加上 /v{version} 前缀。
 *
 * <p>优先级：方法级 {@code @ApiVersion} > 类级 {@code @ApiVersion}。
 * 未标注的控制器或方法不加前缀。
 */
public class VersionedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo original = super.getMappingForMethod(method, handlerType);
        if (original == null) {
            return null;
        }
        // 方法级优先
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        if (apiVersion == null) {
            apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        }
        if (apiVersion == null) {
            return original;
        }
        return prefixPaths(original, "/v" + apiVersion.value());
    }

    private RequestMappingInfo prefixPaths(RequestMappingInfo info, String prefix) {
        var patternsCondition = info.getPatternsCondition();
        if (patternsCondition == null || patternsCondition.getPatterns().isEmpty()) {
            return info;
        }
        return info.mutate()
                .paths(patternsCondition.getPatterns().stream()
                        .map(p -> {
                            String path = p.toString();
                            // 避免重复前缀（已含 /vN 开头时跳过）
                            if (path.startsWith(prefix + "/") || path.equals(prefix)) {
                                return path;
                            }
                            return prefix + path;
                        })
                        .toArray(String[]::new))
                .build();
    }
}
