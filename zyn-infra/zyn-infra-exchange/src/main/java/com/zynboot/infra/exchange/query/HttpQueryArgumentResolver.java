package com.zynboot.infra.exchange.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

import java.util.Collection;
import java.util.Map;

/**
 * 将标注了 {@link HttpQuery} 的 POJO 参数展开为 URL 查询参数。
 * <p>
 * 通过 ObjectMapper 将 POJO 转为 Map，再逐个添加为 requestParameter。
 */
public class HttpQueryArgumentResolver implements HttpServiceArgumentResolver {

    private ObjectMapper mapper;

    public HttpQueryArgumentResolver() {
        this(new ObjectMapper());
    }

    public HttpQueryArgumentResolver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void setObjectMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean resolve(Object argument, MethodParameter parameter,
                           HttpRequestValues.Builder requestValues) {
        if (argument == null || !parameter.hasParameterAnnotation(HttpQuery.class)) {
            return false;
        }

        Map<String, Object> map = mapper.convertValue(argument, new TypeReference<>() {});

        map.forEach((key, value) -> {
            if (value == null) return;
            if (value instanceof Collection<?> col) {
                for (Object item : col) {
                    requestValues.addRequestParameter(key, String.valueOf(item));
                }
            } else {
                requestValues.addRequestParameter(key, String.valueOf(value));
            }
        });
        return true;
    }
}
