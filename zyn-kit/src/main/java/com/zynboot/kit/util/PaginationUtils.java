package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 分页参数计算工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaginationUtils {

    /**
     * 计算总页数。
     */
    public static int totalPages(long totalRows, int pageSize) {
        if (totalRows <= 0 || pageSize <= 0) return 0;
        return (int) Math.ceil((double) totalRows / pageSize);
    }

    /**
     * 计算偏移量（LIMIT offset, size）。
     */
    public static int offset(int pageNum, int pageSize) {
        if (pageNum <= 0) pageNum = 1;
        return (pageNum - 1) * pageSize;
    }

    /**
     * 校正页码（防止越界）。
     */
    public static int normalizePage(int pageNum, int totalPages) {
        if (pageNum <= 0) return 1;
        if (totalPages > 0 && pageNum > totalPages) return totalPages;
        return pageNum;
    }

    /**
     * 校正 pageSize（限制最大值）。
     */
    public static int normalizeSize(int pageSize, int maxSize) {
        if (pageSize <= 0) return 10;
        return Math.min(pageSize, maxSize);
    }

    /**
     * 判断是否有下一页。
     */
    public static boolean hasNext(int pageNum, int totalPages) {
        return pageNum < totalPages;
    }

    /**
     * 判断是否有上一页。
     */
    public static boolean hasPrev(int pageNum) {
        return pageNum > 1;
    }
}
