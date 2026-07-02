package com.zynboot.infra.web.cors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 跨域配置。
 *
 * <pre>
 * zyn:
 *   web:
 *     cors:
 *       allowed-origins: ["*"]
 *       allowed-methods: ["GET","POST","PUT","DELETE","PATCH","OPTIONS"]
 *       allowed-headers: ["*"]
 *       allow-credentials: false
 *       max-age: 3600
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zyn.web.cors")
public class CorsProperties {

    /** 是否启用全局 CORS。 */
    private boolean enabled = true;

    /** 允许的来源（* 表示全部）。 */
    private List<String> allowedOrigins = List.of("*");

    /** 允许的 HTTP 方法。 */
    private List<String> allowedMethods = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    );

    /** 允许的请求头（* 表示全部）。 */
    private List<String> allowedHeaders = List.of("*");

    /** 暴露给前端的响应头。 */
    private List<String> exposedHeaders = List.of();

    /** 是否允许携带凭证（Cookie/Authorization）。 */
    private boolean allowCredentials = false;

    /** 预检请求缓存时间（秒）。 */
    private long maxAge = 3600;
}
