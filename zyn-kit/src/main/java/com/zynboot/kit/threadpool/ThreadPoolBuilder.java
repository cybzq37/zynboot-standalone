package com.zynboot.kit.threadpool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池构建器，提供合理的默认配置。
 *
 * <pre>
 * // 使用默认配置
 * ExecutorService pool = ThreadPoolBuilder.newPool();
 *
 * // 自定义配置
 * ExecutorService pool = ThreadPoolBuilder.create()
 *     .coreSize(4)
 *     .maxSize(16)
 *     .queueCapacity(5000)
 *     .threadPrefix("biz-pool")
 *     .build();
 * </pre>
 */
public final class ThreadPoolBuilder {

    private static final int DEFAULT_CORE_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());

    private int coreSize = DEFAULT_CORE_SIZE;
    private int maxSize = DEFAULT_CORE_SIZE * 2;
    private int queueCapacity = 2000;
    private long keepAliveSeconds = 60;
    private String threadPrefix = "pool";
    private boolean daemon = false;
    private RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.CallerRunsPolicy();

    private ThreadPoolBuilder() {
    }

    /**
     * 创建构建器。
     */
    public static ThreadPoolBuilder create() {
        return new ThreadPoolBuilder();
    }

    /**
     * 使用默认配置直接创建线程池。
     */
    public static ExecutorService newPool() {
        return new ThreadPoolBuilder().build();
    }

    public ThreadPoolBuilder coreSize(int coreSize) {
        this.coreSize = coreSize;
        return this;
    }

    public ThreadPoolBuilder maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public ThreadPoolBuilder queueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }

    public ThreadPoolBuilder keepAliveSeconds(long keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
        return this;
    }

    public ThreadPoolBuilder threadPrefix(String threadPrefix) {
        this.threadPrefix = threadPrefix;
        return this;
    }

    public ThreadPoolBuilder daemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public ThreadPoolBuilder rejectedHandler(RejectedExecutionHandler handler) {
        this.rejectedHandler = handler;
        return this;
    }

    /**
     * 构建线程池。
     */
    public ExecutorService build() {
        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(threadPrefix, daemon),
                rejectedHandler
        );
    }

    /**
     * 命名线程工厂。
     */
    static final class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger index = new AtomicInteger(1);
        private final String prefix;
        private final boolean daemon;

        NamedThreadFactory(String prefix, boolean daemon) {
            this.prefix = (prefix == null || prefix.isBlank()) ? "pool" : prefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + index.getAndIncrement());
            thread.setDaemon(daemon);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
