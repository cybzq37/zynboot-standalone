package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 位运算工具类，用于权限掩码、标志位操作。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BitUtils {

    /**
     * 设置指定位（置 1）。
     *
     * @param flags 原始值
     * @param bit   第几位（从 0 开始）
     */
    public static long set(long flags, int bit) {
        return flags | (1L << bit);
    }

    /**
     * 清除指定位（置 0）。
     */
    public static long clear(long flags, int bit) {
        return flags & ~(1L << bit);
    }

    /**
     * 翻转指定位。
     */
    public static long toggle(long flags, int bit) {
        return flags ^ (1L << bit);
    }

    /**
     * 判断指定位是否为 1。
     */
    public static boolean has(long flags, int bit) {
        return (flags & (1L << bit)) != 0;
    }

    /**
     * 批量设置多个位。
     */
    public static long setBits(long flags, int... bits) {
        for (int bit : bits) flags = set(flags, bit);
        return flags;
    }

    /**
     * 批量清除多个位。
     */
    public static long clearBits(long flags, int... bits) {
        for (int bit : bits) flags = clear(flags, bit);
        return flags;
    }

    /**
     * 计算二进制中 1 的个数（popcount）。
     */
    public static int bitCount(long value) {
        return Long.bitCount(value);
    }

    /**
     * 最高有效位的位置。
     */
    public static int highestBit(long value) {
        return value == 0 ? -1 : 63 - Long.numberOfLeadingZeros(value);
    }

    /**
     * 最低有效位的位置。
     */
    public static int lowestBit(long value) {
        return value == 0 ? -1 : Long.numberOfTrailingZeros(value);
    }

    /**
     * 生成从 from 到 to 的位掩码（包含两端）。
     * <p>
     * {@code mask(0, 3)} → {@code 0b1111} = 15
     */
    public static long mask(int from, int to) {
        return ((1L << (to - from + 1)) - 1) << from;
    }
}
