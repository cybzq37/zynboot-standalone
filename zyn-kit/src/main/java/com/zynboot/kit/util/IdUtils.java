package com.zynboot.kit.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class IdUtils {

    private static final String WORKER_ID_KEY = "zyn.id.worker-id";
    private static final String DATACENTER_ID_KEY = "zyn.id.datacenter-id";
    private static final String WORKER_ID_ENV = "ZYN_ID_WORKER_ID";
    private static final String DATACENTER_ID_ENV = "ZYN_ID_DATACENTER_ID";

    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final AtomicLong SERIAL = new AtomicLong(0);

    private static volatile long serialDay = LocalDate.now().toEpochDay();

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static volatile Snowflake snowflake = new Snowflake(resolveWorkerId(), resolveDatacenterId());

    private IdUtils() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String uuidWithHyphen() {
        return UUID.randomUUID().toString();
    }

    /**
     * 22位左右的短UUID(URL安全)。
     */
    public static String shortUuid() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    /**
     * 指定位数短ID(基于Base62随机)。
     */
    public static String shortUuid(int length) {
        return randomBase62(length);
    }

    public static String randomNumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    public static String randomBase62(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62[SECURE_RANDOM.nextInt(BASE62.length)]);
        }
        return sb.toString();
    }

    public static String timestampId() {
        return System.currentTimeMillis() + randomNumeric(6);
    }

    /**
     * 雪花ID(long)。
     */
    public static long snowflakeId() {
        return snowflake.nextId();
    }

    public static String snowflakeIdStr() {
        return String.valueOf(snowflakeId());
    }

    /**
     * 运行时设置雪花节点参数。
     */
    public static void setSnowflakeNode(long workerId, long datacenterId) {
        snowflake = new Snowflake(workerId, datacenterId);
    }

    /**
     * 使用系统配置(系统属性/环境变量)重新加载雪花节点参数。
     */
    public static void reloadSnowflakeNodeFromConfig() {
        snowflake = new Snowflake(resolveWorkerId(), resolveDatacenterId());
    }

    public static long currentSnowflakeWorkerId() {
        return snowflake.workerId;
    }

    public static long currentSnowflakeDatacenterId() {
        return snowflake.datacenterId;
    }

    /**
     * 流水号(按天重置): yyyyMMdd + 6位序号。
     */
    public static String serialNo() {
        return serialNo("", 6);
    }

    /**
     * 流水号(按天重置): prefix + yyyyMMdd + 指定位数序号。
     */
    public static String serialNo(String prefix, int sequenceLength) {
        if (sequenceLength <= 0) {
            throw new IllegalArgumentException("sequenceLength must be greater than 0");
        }

        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();
        if (todayEpoch != serialDay) {
            synchronized (IdUtils.class) {
                if (todayEpoch != serialDay) {
                    serialDay = todayEpoch;
                    SERIAL.set(0);
                }
            }
        }

        long seq = SERIAL.incrementAndGet();
        String serialPart = String.format("%0" + sequenceLength + "d", seq);
        return (prefix == null ? "" : prefix) + today.format(DAY_FMT) + serialPart;
    }

    private static final class Snowflake {

        private static final long EPOCH = 1704067200000L;
        private static final long WORKER_ID_BITS = 5L;
        private static final long DATACENTER_ID_BITS = 5L;
        private static final long SEQUENCE_BITS = 12L;

        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
        private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
        private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
        private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

        private final long workerId;
        private final long datacenterId;
        private final long idBase;

        // 打包 lastTimestamp(高52位) + sequence(低12位)，用 CAS 原子更新
        private final AtomicLong state = new AtomicLong(-1L);

        private Snowflake(long workerId, long datacenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("workerId out of range");
            }
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException("datacenterId out of range");
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
            this.idBase = (datacenterId << DATACENTER_ID_SHIFT) | (workerId << WORKER_ID_SHIFT);
        }

        private long nextId() {
            while (true) {
                long current = state.get();
                long lastTs = current >>> SEQUENCE_BITS;
                long seq = current & SEQUENCE_MASK;
                long now = timeGen();

                if (now < lastTs) {
                    throw new IllegalStateException("Clock moved backwards. Refusing to generate id");
                }

                long nextSeq;
                long nextTs;
                if (now == lastTs) {
                    nextSeq = (seq + 1) & SEQUENCE_MASK;
                    if (nextSeq == 0) {
                        nextTs = tilNextMillis(lastTs);
                    } else {
                        nextTs = now;
                    }
                } else {
                    nextSeq = 0;
                    nextTs = now;
                }

                long next = (nextTs << SEQUENCE_BITS) | nextSeq;
                if (state.compareAndSet(current, next)) {
                    return ((nextTs - EPOCH) << TIMESTAMP_LEFT_SHIFT) | idBase | nextSeq;
                }
            }
        }

        private long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                Thread.onSpinWait();
                timestamp = timeGen();
            }
            return timestamp;
        }

        private long timeGen() {
            return System.currentTimeMillis();
        }
    }

    private static long resolveWorkerId() {
        return resolveNodeId(WORKER_ID_KEY, WORKER_ID_ENV, 1L, 31L);
    }

    private static long resolveDatacenterId() {
        return resolveNodeId(DATACENTER_ID_KEY, DATACENTER_ID_ENV, 1L, 31L);
    }

    private static long resolveNodeId(String propertyKey, String envKey, long defaultValue, long maxValue) {
        String propertyVal = System.getProperty(propertyKey);
        String raw = (propertyVal != null && !propertyVal.isBlank()) ? propertyVal : System.getenv(envKey);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value < 0 || value > maxValue) {
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
