package com.zynboot.infra.web.version;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * API 版本自动配置：通过 {@link WebMvcRegistrations} 注入自定义的
 * {@link VersionedRequestMappingHandlerMapping}，使 {@code @ApiVersion} 注解自动
 * 为 Controller 路径加上 /v{version} 前缀。
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "zyn.web.api-version", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiVersionAutoConfiguration {

    @Bean
    public WebMvcRegistrations apiVersionWebMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new VersionedRequestMappingHandlerMapping();
            }
        };
    }
}
