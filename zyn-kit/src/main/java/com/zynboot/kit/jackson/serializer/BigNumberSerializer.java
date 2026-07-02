package com.zynboot.kit.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 超出 JS 最大最小值 处理
 *
 * @author lichunqing
 */
@JacksonStdImpl
public final class BigNumberSerializer extends NumberSerializer {

    /**
     * 根据 JS Number.MAX_SAFE_INTEGER 与 Number.MIN_SAFE_INTEGER 得来
     */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;
    private static final BigInteger MAX_SAFE_INTEGER_BIGINT = BigInteger.valueOf(MAX_SAFE_INTEGER);
    private static final BigInteger MIN_SAFE_INTEGER_BIGINT = BigInteger.valueOf(MIN_SAFE_INTEGER);

    /**
     * 提供实例
     */
    public static final BigNumberSerializer INSTANCE = new BigNumberSerializer(Number.class);

    // public — required by @JacksonStdImpl
    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (isSafeNumber(value)) {
            super.serialize(value, gen, provider);
        } else {
            gen.writeString(value.toString());
        }
    }

    private boolean isSafeNumber(Number value) {
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.compareTo(MIN_SAFE_INTEGER_BIGINT) >= 0
                    && bigInteger.compareTo(MAX_SAFE_INTEGER_BIGINT) <= 0;
        }
        long val = value.longValue();
        return val >= MIN_SAFE_INTEGER && val <= MAX_SAFE_INTEGER;
    }
}
