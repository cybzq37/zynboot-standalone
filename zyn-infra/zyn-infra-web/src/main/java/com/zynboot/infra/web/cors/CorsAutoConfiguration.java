package com.zynboot.infra.web.cors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 CORS 跨域自动配置。
 * <p>
 * 通过 {@code zyn.web.cors.enabled=true} 开启（默认开启）。
 * 生产环境可通过 {@code zyn.web.cors.allowed-origins} 限制来源。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(CorsProperties.class)
@ConditionalOnProperty(prefix = "zyn.web.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CorsAutoConfiguration {

    @Configuration
    static class CorsWebMvcConfigurer implements WebMvcConfigurer {

        private final CorsProperties props;

        CorsWebMvcConfigurer(CorsProperties props) {
            this.props = props;
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            if (props.isAllowCredentials() && props.getAllowedOrigins().contains("*")) {
                log.warn("CORS configuration invalid: allowCredentials=true cannot be combined with allowedOrigins=[*]. Forcing allowCredentials=false.");
                props.setAllowCredentials(false);
            }

            registry.addMapping("/**")
                    .allowedOrigins(props.getAllowedOrigins().toArray(new String[0]))
                    .allowedMethods(props.getAllowedMethods().toArray(new String[0]))
                    .allowedHeaders(props.getAllowedHeaders().toArray(new String[0]))
                    .exposedHeaders(props.getExposedHeaders().toArray(new String[0]))
                    .allowCredentials(props.isAllowCredentials())
                    .maxAge(props.getMaxAge());

            log.info("CORS enabled: origins={}, methods={}, credentials={}",
                    props.getAllowedOrigins(), props.getAllowedMethods(), props.isAllowCredentials());
        }
    }
}
