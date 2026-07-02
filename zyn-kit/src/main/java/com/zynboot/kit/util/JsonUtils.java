package com.zynboot.kit.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.jackson.config.JacksonConfig;
import com.zynboot.kit.jackson.module.SensitiveServiceHolder;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具类，基于 Jackson 提供静态方法。
 * <p>
 * 内置与 {@code JacksonConfig} 相同的配置：Long 安全序列化、BigDecimal 转字符串、
 * LocalDateTime 格式化、IEnum 支持、数据脱敏等。
 * <p>
 * 非 Spring 环境可直接使用；Spring 环境下 {@code JacksonConfig} 会自动配置 ObjectMapper。
 *
 * <pre>
 * String json = JsonUtils.toJson(user);
 * User user = JsonUtils.fromJson(json, User.class);
 * List&lt;User&gt; users = JsonUtils.fromJsonArray(json, User.class);
 * </pre>
 */
public final class JsonUtils {

    private static volatile ObjectMapper objectMapper;

    private JsonUtils() {
    }

    /**
     * 获取预配置的 ObjectMapper 实例（线程安全，懒加载）。
     */
    public static ObjectMapper mapper() {
        if (objectMapper == null) {
            synchronized (JsonUtils.class) {
                if (objectMapper == null) {
                    objectMapper = createMapper();
                }
            }
        }
        return objectMapper;
    }

    /**
     * 替换全局 ObjectMapper 实例（启动时调用一次）。
     */
    public static void configure(ObjectMapper mapper) {
        objectMapper = mapper;
    }

    // ==================== 序列化 ====================

    /**
     * 对象转 JSON 字符串。
     *
     * @return JSON 字符串，序列化失败返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * 对象转格式化 JSON 字符串（便于阅读）。
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to pretty JSON", e);
        }
    }

    // ==================== 反序列化 ====================

    /**
     * JSON 字符串转对象。
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * JSON 字符串转对象（泛型类型）。
     *
     * <pre>
     * List&lt;User&gt; users = JsonUtils.fromJson(json, new TypeReference&lt;&gt;() {});
     * Map&lt;String, Object&gt; map = JsonUtils.fromJson(json, new TypeReference&lt;&gt;() {});
     * </pre>
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper().readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + typeRef.getType(), e);
        }
    }

    /**
     * JSON 字符串转 List。
     *
     * <pre>
     * List&lt;User&gt; users = JsonUtils.fromJsonArray(json, User.class);
     * </pre>
     */
    public static <T> List<T> fromJsonArray(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JavaType listType = mapper().getTypeFactory().constructCollectionType(List.class, elementClass);
            return mapper().readValue(json, listType);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to List<" + elementClass.getSimpleName() + ">", e);
        }
    }

    /**
     * JSON 字符串转 Map。
     */
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper().readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to Map", e);
        }
    }

    /**
     * 对象转 Map（先序列化再反序列化）。
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return mapper().convertValue(obj, new TypeReference<>() {});
    }

    /**
     * Map 转指定类型对象。
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        return mapper().convertValue(map, clazz);
    }

    // ==================== 内部 ====================

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = JacksonConfig.configureObjectMapper(new ObjectMapper());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 初始化脱敏服务（默认开启脱敏）
        new SensitiveServiceHolder(() -> true);

        return mapper;
    }

    /**
     * JSON 操作异常。
     */
    public static class JsonException extends RuntimeException {
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
