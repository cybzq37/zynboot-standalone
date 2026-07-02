package com.zynboot.kit.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 多格式 LocalDateTime 反序列化器。
 * <p>
 * 支持的格式：
 * <ul>
 *   <li>{@code yyyy-MM-dd HH:mm:ss}、{@code yyyy-MM-dd'T'HH:mm:ss}</li>
 *   <li>{@code yyyy-MM-dd HH:mm}、{@code yyyy-MM-dd'T'HH:mm}</li>
 *   <li>{@code yyyy/MM/dd HH:mm:ss}、{@code yyyy/MM/dd}</li>
 *   <li>{@code yyyyMMdd}、{@code yyyyMMddHHmmss}</li>
 *   <li>{@code yyyy年MM月dd日}</li>
 *   <li>{@code dd/MM/yyyy}、{@code MM/dd/yyyy}</li>
 *   <li>带时区 {@code yyyy-MM-dd'T'HH:mm:ssZ}</li>
 *   <li>带毫秒 {@code .SSS}</li>
 *   <li>epoch 秒/毫秒数字</li>
 * </ul>
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final LocalDateTimeDeserializer INSTANCE = new LocalDateTimeDeserializer();

    private static final DateTimeFormatter[] FORMATTERS = {
            // 带时区（优先匹配，避免被无时区格式截断）
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"),
            // 带毫秒
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            // 标准日期时间
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss"),
            // 不带秒
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            // 紧凑格式
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
    };

    private static final DateTimeFormatter[] DATE_ONLY_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    };

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 数字时间戳
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return fromTimestamp(p.getLongValue());
        }

        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();

        // 字符串形式的数字时间戳（10位秒 或 13位毫秒）
        if (text.matches("-?\\d{10,13}")) {
            return fromTimestamp(Long.parseLong(text));
        }

        // 带时间部分的格式
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                // 带时区的格式解析为 ZonedDateTime，再转 LocalDateTime
                ZonedDateTime zdt = ZonedDateTime.parse(text, formatter);
                return zdt.toLocalDateTime();
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        // 纯日期格式，补零时间
        for (DateTimeFormatter formatter : DATE_ONLY_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        throw ctxt.weirdStringException(text, LocalDateTime.class,
                "无法解析为 LocalDateTime，支持的格式：yyyy-MM-dd HH:mm:ss、yyyyMMdd、yyyy年MM月dd日、epoch 秒/毫秒等");
    }

    private static LocalDateTime fromTimestamp(long timestamp) {
        Instant instant = timestamp < 1_000_000_000_000L
                ? Instant.ofEpochSecond(timestamp)
                : Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
