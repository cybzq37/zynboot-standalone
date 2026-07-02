package com.zynboot.kit.jackson.module;

import com.zynboot.kit.jackson.plugins.sensitive.SensitiveService;
import jakarta.annotation.PostConstruct;

/**
 * 持有 {@link SensitiveService} 的 Bean 引用。
 * <p>
 * 由于 Jackson 的 {@link SensitiveJsonSerializer} 由 Jackson 直接实例化，
 * 无法通过依赖注入获取 SensitiveService，此类作为桥梁提供访问途径。
 *
 * @author lichunqing
 */
public class SensitiveServiceHolder {

    private static volatile SensitiveService instance;

    private final SensitiveService sensitiveService;

    public SensitiveServiceHolder(SensitiveService sensitiveService) {
        this.sensitiveService = sensitiveService;
    }

    @PostConstruct
    public void init() {
        instance = this.sensitiveService;
    }

    /**
     * 获取 SensitiveService 实例
     *
     * @return SensitiveService，如果容器中不存在则返回 null
     */
    public static SensitiveService getInstance() {
        return instance;
    }
}
