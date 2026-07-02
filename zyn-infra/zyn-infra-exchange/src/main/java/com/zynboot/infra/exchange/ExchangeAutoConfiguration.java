package com.zynboot.infra.exchange;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * HTTP 客户端自动配置。
 * 引入 zyn-infra-exchange 依赖后自动生效。
 * 服务地址配置由 zyn-conf 模块提供。
 */
@AutoConfiguration
@ConditionalOnClass(HttpExchange.class)
@ConditionalOnProperty(prefix = "zyn.exchange", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ExchangeProperties.class)
@Import(ExchangeClientRegistrar.class)
public class ExchangeAutoConfiguration {
}
