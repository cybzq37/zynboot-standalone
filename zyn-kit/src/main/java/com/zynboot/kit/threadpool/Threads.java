package com.zynboot.kit.threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程工具类：全局默认线程池 + 池状态快照 + CompletableFuture 支持。
 */
public final class Threads {

    private static volatile ExecutorService globalPool;

    private Threads() {
    }

    /**
     * 获取全局默认线程池（懒加载）。
     */
    public static ExecutorService globalPool() {
        if (globalPool == null) {
            synchronized (Threads.class) {
                if (globalPool == null) {
                    globalPool = ThreadPoolBuilder.create()
                            .threadPrefix("global")
                            .build();
                }
            }
        }
        return globalPool;
    }

    // ==================== CompletableFuture ====================

    /**
     * 异步执行任务，返回 CompletableFuture。
     */
    public static <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, globalPool());
    }

    /**
     * 异步执行无返回值任务。
     */
    public static CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, globalPool());
    }

    /**
     * 异步执行任务，使用指定线程池。
     */
    public static <T> CompletableFuture<T> submit(Callable<T> task, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * 异步执行无返回值任务，使用指定线程池。
     */
    public static CompletableFuture<Void> run(Runnable task, ExecutorService executor) {
        return CompletableFuture.runAsync(task, executor);
    }

    // ==================== 线程池状态 ====================

    /**
     * 获取线程池状态快照。
     */
    public static PoolStatus status(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor pool) {
            return new PoolStatus(
                    pool.getPoolSize(),
                    pool.getActiveCount(),
                    pool.getCorePoolSize(),
                    pool.getMaximumPoolSize(),
                    pool.getQueue().size(),
                    pool.getQueue().remainingCapacity(),
                    pool.getCompletedTaskCount(),
                    pool.getTaskCount()
            );
        }
        return PoolStatus.UNKNOWN;
    }

    /**
     * 获取全局线程池状态。
     */
    public static PoolStatus globalStatus() {
        return status(globalPool());
    }

    /**
     * 线程池状态快照。
     */
    public record PoolStatus(
            int poolSize,
            int activeThreads,
            int coreSize,
            int maxSize,
            int queueSize,
            int queueRemainingCapacity,
            long completedTasks,
            long totalTasks
    ) {
        static final PoolStatus UNKNOWN = new PoolStatus(-1, -1, -1, -1, -1, -1, -1, -1);

        public double queueUsage() {
            if (queueSize < 0 || queueRemainingCapacity < 0) return -1;
            int total = queueSize + queueRemainingCapacity;
            return total == 0 ? 0.0 : (double) queueSize / total;
        }

        @Override
        public String toString() {
            if (this == UNKNOWN) return "PoolStatus{unknown}";
            return "PoolStatus{pool=%d, active=%d, core=%d, max=%d, queue=%d/%d, completed=%d, total=%d}"
                    .formatted(poolSize, activeThreads, coreSize, maxSize,
                            queueSize, queueSize + queueRemainingCapacity,
                            completedTasks, totalTasks);
        }
    }
}
