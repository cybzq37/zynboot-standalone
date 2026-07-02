package com.zynboot.kit.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.zynboot.kit.enums.IEnum;

import java.io.IOException;
import java.util.Map;

/**
 * {@link IEnum} 通用反序列化器。
 * <p>
 * 支持两种 JSON 格式：
 * <ul>
 *   <li>对象格式：{@code {"code": 1, "desc": "男"}}</li>
 *   <li>code 值格式：{@code 1} 或 {@code "ACTIVE"}</li>
 * </ul>
 */
public class IEnumDeserializer extends JsonDeserializer<IEnum<?>> implements ContextualDeserializer {

    private final Class<?> targetType;

    public IEnumDeserializer() {
        this(null);
    }

    public IEnumDeserializer(Class<?> targetType) {
        this.targetType = targetType;
    }

    @Override
    public IEnum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Class<?> type = this.targetType;
        if (type == null) {
            // 兜底：尝试从 context 获取
            if (ctxt.getContextualType() != null) {
                type = ctxt.getContextualType().getRawClass();
            }
        }
        if (type == null || !IEnum.class.isAssignableFrom(type)) {
            throw JsonMappingException.from(p, "无法确定 IEnum 目标类型");
        }

        if (p.currentToken() == JsonToken.START_OBJECT) {
            Map<?, ?> map = p.readValueAs(Map.class);
            Object code = map.get("code");
            if (code == null) {
                throw JsonMappingException.from(p, "IEnum 对象缺少 code 字段");
            }
            return ofCode(type, code);
        }

        return ofCode(type, p.getValueAsString());
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        if (this.targetType != null) {
            return this;
        }
        Class<?> type = null;
        if (property != null) {
            type = property.getType().getRawClass();
        }
        if (type == null && ctxt.getContextualType() != null) {
            type = ctxt.getContextualType().getRawClass();
        }
        if (type != null && IEnum.class.isAssignableFrom(type)) {
            return new IEnumDeserializer(type);
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IEnum<?> ofCode(Class<?> enumType, Object code) {
        return (IEnum<?>) IEnum.ofCode((Class) enumType, code);
    }
}
