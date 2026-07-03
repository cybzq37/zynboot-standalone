package com.zynboot.map.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Zyn Map API")
                .version("v1.0.0")
                .description("地图服务接口，覆盖图层、要素、瓦片、导入导出、空间分析与公开发布能力。"));
    }
}
