package com.zynboot.kit.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.zynboot.kit.jackson.plugins.sensitive.Sensitive;
import com.zynboot.kit.jackson.plugins.sensitive.SensitiveService;
import com.zynboot.kit.jackson.plugins.sensitive.SensitiveStrategy;
import com.zynboot.kit.jackson.module.SensitiveServiceHolder;

import java.io.IOException;

/**
 * 数据脱敏 JSON 序列化器
 * <p>
 * 通过 {@link SensitiveServiceHolder} 获取 Spring 管理的 {@link SensitiveService}，
 * 避免在 Jackson 直接实例化的序列化器上使用 {@code @Autowired}。
 *
 * @author Yjoioooo
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final SensitiveStrategy strategy;
    private final SensitiveService sensitiveService;

    public SensitiveJsonSerializer() {
        this(null, null);
    }

    private SensitiveJsonSerializer(SensitiveStrategy strategy, SensitiveService sensitiveService) {
        this.strategy = strategy;
        this.sensitiveService = sensitiveService;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (strategy == null || !shouldDesensitize()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(strategy.mask(value));
    }

    private boolean shouldDesensitize() {
        SensitiveService service = sensitiveService;
        if (service == null) {
            service = SensitiveServiceHolder.getInstance();
        }
        return service == null || service.isSensitive();
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }

        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Sensitive.class);
        }
        if (annotation != null && String.class.equals(property.getType().getRawClass())) {
            SensitiveService service = SensitiveServiceHolder.getInstance();
            return new SensitiveJsonSerializer(annotation.strategy(), service);
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
