package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间工具类
 *
 * @author lichunqing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {

    public static final String YYYY = "yyyy";

    public static final String YYYY_MM = "yyyy-MM";

    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

    public static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private static final String[] DATE_TIME_PATTERNS = {
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd'T'HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm"
    };

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "yyyyMMdd",
        "yyyy年MM月dd日",
        "dd/MM/yyyy",
        "MM/dd/yyyy"
    };


    public static Date now() {
        return Date.from(Instant.now());
    }

    public static String date() {
        return format(LocalDate.now(), "yyyyMMdd");
    }

    public static String dateHour() {
        return format(LocalDateTime.now(), "yyyyMMddHH");
    }

    public static String dateMinute() {
        return format(LocalDateTime.now(), "yyyyMMddHHmm");
    }

    public static String datetime() {
        return format(LocalDateTime.now(), YYYYMMDDHHMMSS);
    }


    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String dateHyp() {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static String getTime() {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static String dateTimeNow() {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static String dateTimeNow(final String format) {
        return format(LocalDateTime.now(), format);
    }

    public static String date(final Date date) {
        return format(date, YYYY_MM_DD);
    }

    public static String parseDateToStr(final String format, final Date date) {
        return format(date, format);
    }

    public static Date date(final String format, final String ts) {
        return parseToDate(ts, format);
    }

    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static String datePath() {
        return format(LocalDate.now(), "yyyy/MM/dd");
    }



    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseToDate(str.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 计算两个时间的差值，返回格式化的中文描述。
     */
    public static String formatDuration(Date endDate, Date startDate) {
        if (endDate == null || startDate == null) {
            return "0天0小时0分钟";
        }
        long diff = endDate.getTime() - startDate.getTime();
        long day = diff / (1000 * 60 * 60 * 24);
        long hour = diff % (1000 * 60 * 60 * 24) / (1000 * 60 * 60);
        long min = diff % (1000 * 60 * 60) / (1000 * 60);
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 增加 LocalDateTime ==> Date
     */
    public static Date toDate(LocalDateTime temporalAccessor) {
        if (temporalAccessor == null) {
            return null;
        }
        ZonedDateTime zdt = temporalAccessor.atZone(SYSTEM_ZONE);
        return Date.from(zdt.toInstant());
    }

    /**
     * 增加 LocalDate ==> Date
     */
    public static Date toDate(LocalDate temporalAccessor) {
        if (temporalAccessor == null) {
            return null;
        }
        LocalDateTime localDateTime = LocalDateTime.of(temporalAccessor, LocalTime.of(0, 0, 0));
        ZonedDateTime zdt = localDateTime.atZone(SYSTEM_ZONE);
        return Date.from(zdt.toInstant());
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), SYSTEM_ZONE);
    }

    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return format(toLocalDateTime(date), pattern);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(formatter(pattern));
    }

    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(formatter(pattern));
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return format(dateTime, YYYY_MM_DD_HH_MM_SS);
    }

    public static String formatDate(Date date) {
        return format(date, YYYY_MM_DD);
    }

    public static Date parseToDate(String text) {
        LocalDateTime ldt = parseToLocalDateTime(text);
        return toDate(ldt);
    }

    public static Date parseToDate(String text, String pattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(text.trim(), formatter(pattern));
            return toDate(ldt);
        } catch (DateTimeParseException ex) {
            LocalDate localDate = LocalDate.parse(text.trim(), formatter(pattern));
            return toDate(localDate.atStartOfDay());
        }
    }

    public static LocalDateTime parseToLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();

        LocalDateTime epochParsed = parseEpoch(value);
        if (epochParsed != null) {
            return epochParsed;
        }

        LocalDateTime isoParsed = parseIso(value);
        if (isoParsed != null) {
            return isoParsed;
        }

        for (String pattern : DATE_TIME_PATTERNS) {
            try {
                return LocalDateTime.parse(value, formatter(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }

        for (String pattern : DATE_PATTERNS) {
            try {
                return LocalDate.parse(value, formatter(pattern)).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Unsupported datetime format: " + text);
    }

    public static LocalDateTime parseToLocalDateTime(String text, String pattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            return LocalDateTime.parse(value, formatter(pattern));
        } catch (DateTimeParseException ex) {
            LocalDate localDate = LocalDate.parse(value, formatter(pattern));
            return localDate.atStartOfDay();
        }
    }

    public static LocalDate parseToLocalDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (String pattern : DATE_PATTERNS) {
            try {
                return LocalDate.parse(value, formatter(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        return parseToLocalDateTime(value).toLocalDate();
    }

    public static LocalDateTime startOfToday(int offsetDays) {
        return LocalDate.now().plusDays(offsetDays).atStartOfDay();
    }

    public static LocalDateTime endOfToday(int offsetDays) {
        return LocalDate.now().plusDays(offsetDays).atTime(LocalTime.MAX);
    }

    public static Date startOfTodayDate(int offsetDays) {
        return toDate(startOfToday(offsetDays));
    }

    public static Date endOfTodayDate(int offsetDays) {
        return toDate(endOfToday(offsetDays));
    }

    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atTime(LocalTime.MAX);
    }

    public static Date startOfDay(Date date) {
        LocalDateTime dateTime = toLocalDateTime(date);
        return toDate(startOfDay(dateTime));
    }

    public static Date endOfDay(Date date) {
        LocalDateTime dateTime = toLocalDateTime(date);
        return toDate(endOfDay(dateTime));
    }

    /**
     * 判断 left 是否大于 right。
     */
    public static boolean isAfter(Object left, Object right) {
        return isAfter(left, right, 0);
    }

    /**
     * 判断 left 是否大于 right + offsetSeconds。
     */
    public static boolean isAfter(Object left, Object right, long offsetSeconds) {
        LocalDateTime leftTime = toLocalDateTime(left);
        LocalDateTime rightTime = toLocalDateTime(right);
        return leftTime.isAfter(rightTime.plusSeconds(offsetSeconds));
    }

    /**
     * 判断 left 是否小于 right。
     */
    public static boolean isBefore(Object left, Object right) {
        return isBefore(left, right, 0);
    }

    /**
     * 判断 left 是否小于 right - offsetSeconds。
     */
    public static boolean isBefore(Object left, Object right, long offsetSeconds) {
        LocalDateTime leftTime = toLocalDateTime(left);
        LocalDateTime rightTime = toLocalDateTime(right);
        return leftTime.isBefore(rightTime.minusSeconds(offsetSeconds));
    }

    /**
     * 判断 left 是否大于等于 right。
     */
    public static boolean isAfterOrEqual(Object left, Object right) {
        LocalDateTime leftTime = toLocalDateTime(left);
        LocalDateTime rightTime = toLocalDateTime(right);
        return !leftTime.isBefore(rightTime);
    }

    /**
     * 判断 left 是否小于等于 right。
     */
    public static boolean isBeforeOrEqual(Object left, Object right) {
        LocalDateTime leftTime = toLocalDateTime(left);
        LocalDateTime rightTime = toLocalDateTime(right);
        return !leftTime.isAfter(rightTime);
    }

    /**
     * 统一时间入参转换，支持 Date / LocalDateTime / LocalDate / String。
     */
    public static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("time value cannot be null");
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Date date) {
            return toLocalDateTime(date);
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof String text) {
            LocalDateTime parsed = parseToLocalDateTime(text);
            if (parsed == null) {
                throw new IllegalArgumentException("Unsupported datetime value: " + text);
            }
            return parsed;
        }
        throw new IllegalArgumentException("Unsupported time type: " + value.getClass().getName());
    }

    public static List<String> supportedPatterns() {
        List<String> patterns = new ArrayList<>(DATE_TIME_PATTERNS.length + DATE_PATTERNS.length);
        Collections.addAll(patterns, DATE_TIME_PATTERNS);
        Collections.addAll(patterns, DATE_PATTERNS);
        return patterns;
    }

    private static DateTimeFormatter formatter(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("pattern cannot be blank");
        }
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    private static LocalDateTime parseIso(String value) {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }

        try {
            Instant instant = Instant.parse(value);
            return LocalDateTime.ofInstant(instant, SYSTEM_ZONE);
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private static LocalDateTime parseEpoch(String value) {
        if (!value.matches("^-?\\d{10,13}$")) {
            return null;
        }
        long epoch = Long.parseLong(value);
        if (value.length() == 10) {
            epoch = epoch * 1000;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), SYSTEM_ZONE);
    }
}
