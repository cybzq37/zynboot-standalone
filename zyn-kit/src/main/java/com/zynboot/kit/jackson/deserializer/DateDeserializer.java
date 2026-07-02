package com.zynboot.kit.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * 多格式 Date 反序列化器。
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
public class DateDeserializer extends JsonDeserializer<Date> {

    public static final DateDeserializer INSTANCE = new DateDeserializer();

    private static final String[] PATTERNS = {
            // 带时区
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            // 带毫秒
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            // 标准日期时间
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd'T'HH:mm:ss",
            // 不带秒
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy/MM/dd HH:mm",
            // 纯日期（非紧凑）
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy年MM月dd日",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
    };

    // 紧凑格式用 DateTimeFormatter 解析（SimpleDateFormat 的 yyyy 会贪婪消费数字）
    private static final DateTimeFormatter COMPACT_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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

        // 紧凑格式 yyyyMMddHHmmss（14位纯数字）
        if (text.matches("\\d{14}")) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(text, COMPACT_DATETIME);
                return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }

        // 紧凑格式 yyyyMMdd（8位纯数字）
        if (text.matches("\\d{8}")) {
            try {
                LocalDate ld = LocalDate.parse(text, COMPACT_DATE);
                return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }

        // 其他格式
        for (String pattern : PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(text);
            } catch (ParseException ignored) {
            }
        }

        throw ctxt.weirdStringException(text, Date.class,
                "无法解析为 Date，支持的格式：yyyy-MM-dd HH:mm:ss、yyyyMMdd、yyyy年MM月dd日、epoch 秒/毫秒等");
    }

    private static Date fromTimestamp(long timestamp) {
        long millis = timestamp < 1_000_000_000_000L
                ? timestamp * 1000
                : timestamp;
        return new Date(millis);
    }
}
