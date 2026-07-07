package com.zynboot.kit.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 兼容 JSON 对象与 JSON 字符串两种输入的 {@link JsonNode} 反序列化器。
 * <p>
 * 当输入为 JSON 字符串时，自动解析为 {@link JsonNode} 对象树，
 * 避免 {@code TextNode} 在后续 {@code toJson} 序列化时被额外包裹一层引号。
 * </p>
 * <p>
 * 典型场景：前端可能传 {@code {"type":"Point","coordinates":[...]}}（对象），
 * 也可能传 {@code "{\"type\":\"Point\",...}"}（字符串），均能正确接收。
 * </p>
 *
 * <pre>{@code
 * @JsonDeserialize(using = LenientJsonNodeDeserializer.class)
 * private JsonNode geometry;
 * }</pre>
 *
 * @author zyn
 * @see com.fasterxml.jackson.databind.annotation.JsonDeserialize
 */
public class LenientJsonNodeDeserializer extends JsonDeserializer<JsonNode> {

    /** 单例复用 */
    public static final LenientJsonNodeDeserializer INSTANCE = new LenientJsonNodeDeserializer();

    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 字符串：尝试解析为 JSON 对象树
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            try {
                return mapper.readTree(text);
            } catch (Exception e) {
                throw ctxt.weirdStringException(text, JsonNode.class, "不是合法的 JSON");
            }
        }
        // 其他类型（对象/数组/数字等）：直接读取为 JsonNode 树
        return p.readValueAsTree();
    }
}
