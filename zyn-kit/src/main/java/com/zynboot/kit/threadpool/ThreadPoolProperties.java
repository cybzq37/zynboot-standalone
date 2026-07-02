package com.zynboot.kit.threadpool;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "zyn.threadpool")
public class ThreadPoolProperties {

    /** 核心线程数。 */
    private int coreSize = Math.max(2, Runtime.getRuntime().availableProcessors());

    /** 最大线程数。 */
    private int maxSize = coreSize * 2;

    /** 队列容量。 */
    private int queueCapacity = 2000;

    /** 空闲线程存活时间（秒）。 */
    private long keepAliveSeconds = 60;

    /** 线程名前缀。 */
    private String prefix = "pool";

    /** 是否守护线程。 */
    private boolean daemon = false;
}
