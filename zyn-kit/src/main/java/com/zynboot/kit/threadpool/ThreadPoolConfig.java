package com.zynboot.kit.threadpool;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfig {

    @Bean(name = "taskExecutor", destroyMethod = "")
    @ConditionalOnMissingBean(ExecutorService.class)
    public ExecutorService taskExecutor(ThreadPoolProperties props) {
        ExecutorService pool = ThreadPoolBuilder.create()
                .coreSize(props.getCoreSize())
                .maxSize(props.getMaxSize())
                .queueCapacity(props.getQueueCapacity())
                .keepAliveSeconds(props.getKeepAliveSeconds())
                .threadPrefix(props.getPrefix())
                .daemon(props.isDaemon())
                .build();
        log.info("Thread pool created: core={}, max={}, queue={}, prefix={}",
                props.getCoreSize(), props.getMaxSize(),
                props.getQueueCapacity(), props.getPrefix());
        return pool;
    }

    /**
     * 优雅关闭：等待任务完成，超时后强制终止。
     */
    @PreDestroy
    public void shutdown() {
        ExecutorService pool = Threads.globalPool();
        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Thread pool did not terminate in 30s, forcing shutdown");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
