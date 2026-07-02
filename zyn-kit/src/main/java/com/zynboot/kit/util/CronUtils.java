package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cron 表达式工具类。
 * <p>
 * 支持标准 5 位 Cron：分 时 日 月 周
 * 示例：{@code 0 9 * * 1-5}（周一到周五每天 9:00）
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CronUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 判断 Cron 表达式是否合法。
     */
    public static boolean isValid(String cron) {
        if (StringUtils.isBlank(cron)) return false;
        String[] parts = cron.trim().split("\\s+");
        return parts.length == 5 || parts.length == 6;
    }

    /**
     * 计算下次执行时间（从当前时间算起）。
     *
     * @return 下次执行时间的 Instant，解析失败返回 null
     */
    public static Instant nextExecution(String cron) {
        return nextExecution(cron, Instant.now());
    }

    /**
     * 计算从指定时间算起的下次执行时间。
     */
    public static Instant nextExecution(String cron, Instant from) {
        if (!isValid(cron) || from == null) return null;
        try {
            org.springframework.scheduling.support.CronExpression expression =
                    org.springframework.scheduling.support.CronExpression.parse(toSpring6Cron(cron));
            java.time.Instant next = expression.next(from);
            return next;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算未来 N 次执行时间。
     */
    public static List<Instant> nextExecutions(String cron, int count) {
        return nextExecutions(cron, Instant.now(), count);
    }

    public static List<Instant> nextExecutions(String cron, Instant from, int count) {
        List<Instant> times = new ArrayList<>();
        if (!isValid(cron) || from == null || count <= 0) return times;
        try {
            org.springframework.scheduling.support.CronExpression expression =
                    org.springframework.scheduling.support.CronExpression.parse(toSpring6Cron(cron));
            Instant current = from;
            for (int i = 0; i < count; i++) {
                current = expression.next(current);
                if (current == null) break;
                times.add(current);
            }
        } catch (Exception ignored) {
        }
        return times;
    }

    /**
     * Cron 表达式人类可读描述。
     */
    public static String describe(String cron) {
        if (StringUtils.isBlank(cron)) return "";
        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 5) return cron;
        String min = parts[0], hour = parts[1], dom = parts[2], month = parts[3], dow = parts[4];
        StringBuilder sb = new StringBuilder();
        if ("*".equals(min) && "*".equals(hour)) {
            sb.append("每分钟");
        } else if ("*".equals(hour)) {
            sb.append("每小时第").append(min).append("分钟");
        } else {
            sb.append(String.format("每天 %s:%s", pad(hour), pad(min)));
        }
        if (!"*".equals(dom)) sb.append("，每月").append(dom).append("日");
        if (!"*".equals(month)) sb.append("，").append(month).append("月");
        if (!"*".equals(dow) && !"7".equals(dow)) {
            sb.append("，").append(describeDow(dow));
        }
        return sb.toString();
    }

    /**
     * 将 5 位 Cron 转为 Spring 6 的 6 位格式（加秒位）。
     */
    private static String toSpring6Cron(String cron) {
        String trimmed = cron.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 5) return "0 " + trimmed;
        return trimmed;
    }

    private static String pad(String s) {
        return s.length() == 1 ? "0" + s : s;
    }

    private static String describeDow(String dow) {
        return switch (dow) {
            case "1" -> "周一";
            case "2" -> "周二";
            case "3" -> "周三";
            case "4" -> "周四";
            case "5" -> "周五";
            case "6" -> "周六";
            case "0", "7" -> "周日";
            case "1-5" -> "工作日";
            case "0,6", "6,0" -> "周末";
            default -> "周" + dow;
        };
    }
}
