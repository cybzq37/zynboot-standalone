package com.zynboot.infra.mybatis.handler;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.ibatis.logging.Log;

import java.lang.reflect.Type;

/**
 * JSON 类型处理器共享逻辑。
 * <p>
 * 提供 ObjectMapper 管理和 JSON 序列化/反序列化，供 {@link JsonTypeHandler} 和 {@link PgJsonbTypeHandler} 复用。
 */
public final class JacksonHandlerUtils {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JacksonHandlerUtils() {
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper should not be null");
        OBJECT_MAPPER = objectMapper;
    }

    public static <T> T parse(String json, Type fieldType, Log log) {
        ObjectMapper objectMapper = getObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        JavaType javaType = typeFactory.constructType(fieldType);
        try {
            return objectMapper.readValue(json, javaType);
        } catch (JacksonException e) {
            log.error("deserialize json: " + json + " to " + javaType + " error", e);
            throw new RuntimeException(e);
        }
    }

    public static String toJson(Object obj, Log log) {
        try {
            String str = getObjectMapper().writeValueAsString(obj);
            return "null".equals(str) ? null : str;
        } catch (JsonProcessingException e) {
            log.error("serialize " + obj + " to json error", e);
            throw new RuntimeException(e);
        }
    }
}
