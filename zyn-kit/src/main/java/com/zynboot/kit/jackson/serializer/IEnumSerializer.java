package com.zynboot.kit.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zynboot.kit.enums.IEnum;

import java.io.IOException;

public class IEnumSerializer extends JsonSerializer<IEnum<?>> {

    @Override
    public void serialize(IEnum<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeFieldName("code");
        gen.writeObject(value.getCode());
        gen.writeFieldName("desc");
        gen.writeString(value.getDesc());
        gen.writeEndObject();
    }
}
