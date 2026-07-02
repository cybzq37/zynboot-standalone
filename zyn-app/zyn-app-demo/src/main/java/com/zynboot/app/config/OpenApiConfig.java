package com.zynboot.app.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Zyn Template API")
                .version("v1.0.0")
                .description("Java 21 + Spring Boot 3.5.x multi-module template"));
    }
}
