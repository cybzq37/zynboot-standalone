package com.zynboot.kit.jackson.deserializer;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.zynboot.kit.enums.IEnum;

import java.io.IOException;

/**
 * {@link IEnum} 作为 Map key 时的反序列化器。
 * <p>
 * 将 key 字符串按 code 值匹配为对应的枚举常量。
 */
public class IEnumKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        if (ctxt.getContextualType() == null) {
            return key;
        }
        Class<?> targetType = ctxt.getContextualType().getRawClass();
        if (targetType == null || !IEnum.class.isAssignableFrom(targetType)) {
            return key;
        }
        return ofCode(targetType, key);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IEnum<?> ofCode(Class<?> enumType, Object code) {
        return (IEnum<?>) IEnum.ofCode((Class) enumType, code);
    }
}
