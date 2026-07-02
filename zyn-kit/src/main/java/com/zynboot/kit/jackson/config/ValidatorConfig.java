package com.zynboot.kit.jackson.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * 校验框架配置类
 *
 * @author lichunqing
 */
@AutoConfiguration
public class ValidatorConfig {

    /**
     * 配置校验框架快速失败模式（fail_fast）
     * <p>
     * 优先使用 application.properties 中的
     * {@code spring.jpa.properties.hibernate.validator.fail_fast=true}，
     * 此处作为兜底默认值。
     */
    @Bean
    public ValidationConfigurationCustomizer validationConfigurationCustomizer() {
        return configuration -> configuration.addProperty("hibernate.validator.fail_fast", "true");
    }
}
