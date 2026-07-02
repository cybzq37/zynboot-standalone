package com.zynboot.infra.web.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ResponseProperties.class)
@ConditionalOnProperty(prefix = "zyn.web.response", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResponseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalResponseBodyAdvice globalResponseBodyAdvice(ResponseProperties properties, ObjectMapper objectMapper) {
        return new GlobalResponseBodyAdvice(properties, objectMapper);
    }
}
