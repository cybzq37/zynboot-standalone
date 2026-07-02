package com.zynboot.kit.jackson.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final ObjectMapper objectMapper = createObjectMapper();

    @Test
    void shouldSerializeUnsafeNumbersAsStrings() throws Exception {
        NumberPayload payload = new NumberPayload(
                9007199254740991L,
                9007199254740992L,
                new BigInteger("9007199254740992"),
                new BigInteger("100"),
                new BigDecimal("12345678901234567890.12")
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(payload));

        assertThat(json.get("safeLong").isNumber()).isTrue();
        assertThat(json.get("unsafeLong").isTextual()).isTrue();
        assertThat(json.get("unsafeBigInteger").isTextual()).isTrue();
        assertThat(json.get("safeBigInteger").isNumber()).isTrue();
        assertThat(json.get("bigDecimal").isTextual()).isTrue();
        assertThat(json.get("bigDecimal").textValue()).isEqualTo("12345678901234567890.12");
    }

    @Test
    void shouldSerializeDateTimesWithDefaultFormats() throws Exception {
        DatePayload payload = new DatePayload(
                LocalDateTime.of(2026, 5, 28, 13, 45, 30),
                LocalDate.of(2026, 5, 28),
                LocalTime.of(13, 45, 30),
                Instant.parse("2026-05-28T05:45:30Z"),
                Date.from(Instant.parse("2026-05-28T05:45:30Z")),
                ZonedDateTime.of(2026, 5, 28, 13, 45, 30, 0, ZoneId.of("Asia/Shanghai"))
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(payload));

        // LocalDateTime 默认格式 yyyy-MM-dd HH:mm:ss
        assertThat(json.get("localDateTime").textValue()).isEqualTo("2026-05-28 13:45:30");
        // LocalDate/LocalTime 保持 ISO 格式
        assertThat(json.get("localDate").textValue()).isEqualTo("2026-05-28");
        assertThat(json.get("localTime").textValue()).isEqualTo("13:45:30");
        // Instant/Date/ZonedDateTime 输出 ISO 字符串
        assertThat(json.get("instant").textValue()).isEqualTo("2026-05-28T05:45:30Z");
        assertThat(json.get("date").isTextual()).isTrue();
        assertThat(json.get("zonedDateTime").isTextual()).isTrue();
    }

    @Test
    void shouldDeserializeLocalDateTimeFromMultipleFormats() throws Exception {
        LocalDateTime expected = LocalDateTime.of(2026, 5, 28, 13, 45, 30);
        LocalDateTime expectedNoSec = LocalDateTime.of(2026, 5, 28, 13, 45, 0);
        LocalDateTime expectedDateOnly = LocalDateTime.of(2026, 5, 28, 0, 0, 0);

        // 标准格式
        assertThat(parseLdt("{\"v\":\"2026-05-28 13:45:30\"}")).isEqualTo(expected);
        assertThat(parseLdt("{\"v\":\"2026-05-28T13:45:30\"}")).isEqualTo(expected);
        assertThat(parseLdt("{\"v\":\"2026/05/28 13:45:30\"}")).isEqualTo(expected);
        // 不带秒
        assertThat(parseLdt("{\"v\":\"2026-05-28 13:45\"}")).isEqualTo(expectedNoSec);
        assertThat(parseLdt("{\"v\":\"2026-05-28T13:45\"}")).isEqualTo(expectedNoSec);
        assertThat(parseLdt("{\"v\":\"2026/05/28 13:45\"}")).isEqualTo(expectedNoSec);
        // 带毫秒
        assertThat(parseLdt("{\"v\":\"2026-05-28 13:45:30.123\"}"))
                .isEqualTo(LocalDateTime.of(2026, 5, 28, 13, 45, 30, 123_000_000));
        // 带时区
        assertThat(parseLdt("{\"v\":\"2026-05-28T13:45:30+08:00\"}")).isEqualTo(expected);
        assertThat(parseLdt("{\"v\":\"2026-05-28T13:45:30+0800\"}")).isEqualTo(expected);
        // 紧凑格式
        assertThat(parseLdt("{\"v\":\"20260528134530\"}")).isEqualTo(expected);
        // 纯日期
        assertThat(parseLdt("{\"v\":\"2026-05-28\"}")).isEqualTo(expectedDateOnly);
        assertThat(parseLdt("{\"v\":\"2026/05/28\"}")).isEqualTo(expectedDateOnly);
        assertThat(parseLdt("{\"v\":\"20260528\"}")).isEqualTo(expectedDateOnly);
        // 中文格式
        assertThat(parseLdt("{\"v\":\"2026年05月28日\"}")).isEqualTo(expectedDateOnly);
        // 欧洲格式
        assertThat(parseLdt("{\"v\":\"28/05/2026\"}")).isEqualTo(expectedDateOnly);
        // 美国格式
        assertThat(parseLdt("{\"v\":\"05/28/2026\"}")).isEqualTo(expectedDateOnly);
    }

    private LocalDateTime parseLdt(String json) throws Exception {
        return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, LocalDateTime>>() {})
                .get("v");
    }

    @Test
    void shouldDeserializeLocalDateTimeFromTimestamp() throws Exception {
        record Wrapper(LocalDateTime localDateTime) {}

        // epoch 毫秒
        LocalDateTime r1 = objectMapper.readValue("{\"localDateTime\":1780034730000}", Wrapper.class).localDateTime();
        assertThat(r1.getYear()).isEqualTo(2026);

        // epoch 秒
        LocalDateTime r2 = objectMapper.readValue("{\"localDateTime\":1780034730}", Wrapper.class).localDateTime();
        assertThat(r2.getYear()).isEqualTo(2026);
    }

    @Test
    void shouldDeserializeDateFromMultipleFormats() throws Exception {
        Date expected = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-05-28 13:45:30");
        Date expectedNoSec = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-05-28 13:45");
        Date expectedDateOnly = new SimpleDateFormat("yyyy-MM-dd").parse("2026-05-28");

        // 标准格式
        assertThat(parseDate("{\"v\":\"2026-05-28 13:45:30\"}")).isEqualTo(expected);
        assertThat(parseDate("{\"v\":\"2026-05-28T13:45:30\"}")).isEqualTo(expected);
        assertThat(parseDate("{\"v\":\"2026/05/28 13:45:30\"}")).isEqualTo(expected);
        // 不带秒
        assertThat(parseDate("{\"v\":\"2026-05-28 13:45\"}")).isEqualTo(expectedNoSec);
        assertThat(parseDate("{\"v\":\"2026-05-28T13:45\"}")).isEqualTo(expectedNoSec);
        assertThat(parseDate("{\"v\":\"2026/05/28 13:45\"}")).isEqualTo(expectedNoSec);
        // 带毫秒
        assertThat(parseDate("{\"v\":\"2026-05-28 13:45:30.123\"}"))
                .isEqualTo(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse("2026-05-28 13:45:30.123"));
        // 带时区
        assertThat(parseDate("{\"v\":\"2026-05-28T13:45:30+08:00\"}")).isEqualTo(expected);
        assertThat(parseDate("{\"v\":\"2026-05-28T13:45:30+0800\"}")).isEqualTo(expected);
        // 紧凑格式
        assertThat(parseDate("{\"v\":\"20260528134530\"}")).isEqualTo(expected);
        // 纯日期
        assertThat(parseDate("{\"v\":\"2026-05-28\"}")).isEqualTo(expectedDateOnly);
        assertThat(parseDate("{\"v\":\"2026/05/28\"}")).isEqualTo(expectedDateOnly);
        assertThat(parseDate("{\"v\":\"20260528\"}")).isEqualTo(expectedDateOnly);
        // 中文格式
        assertThat(parseDate("{\"v\":\"2026年05月28日\"}")).isEqualTo(expectedDateOnly);
        // 欧洲格式
        assertThat(parseDate("{\"v\":\"28/05/2026\"}")).isEqualTo(expectedDateOnly);
        // 美国格式
        assertThat(parseDate("{\"v\":\"05/28/2026\"}")).isEqualTo(expectedDateOnly);
        // epoch 毫秒
        assertThat(parseDate("{\"v\":" + expected.getTime() + "}")).isEqualTo(expected);
        // epoch 秒
        long epochSecond = expected.getTime() / 1000;
        assertThat(parseDate("{\"v\":" + epochSecond + "}")).isEqualTo(expected);
    }

    private Date parseDate(String json) throws Exception {
        return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Date>>() {})
                .get("v");
    }

    private static ObjectMapper createObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().customizer().customize(builder);
        return builder.build();
    }

    private record NumberPayload(
            Long safeLong,
            Long unsafeLong,
            BigInteger unsafeBigInteger,
            BigInteger safeBigInteger,
            BigDecimal bigDecimal
    ) {
    }

    private record DatePayload(
            LocalDateTime localDateTime,
            LocalDate localDate,
            LocalTime localTime,
            Instant instant,
            Date date,
            ZonedDateTime zonedDateTime
    ) {
    }
}
