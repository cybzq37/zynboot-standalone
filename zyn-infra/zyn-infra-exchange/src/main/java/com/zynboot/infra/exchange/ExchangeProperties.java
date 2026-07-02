package com.zynboot.infra.exchange;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务发现配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zyn.exchange")
public class ExchangeProperties {

    /** 连接超时（毫秒）。 */
    private long connectTimeoutMs = 3000;

    /** 读取超时（毫秒）。 */
    private long readTimeoutMs = 10000;

    /** 是否跟随重定向（微服务内部一般 false）。 */
    private boolean followRedirects = false;

    /** 是否转发当前请求的 Authorization header 到下游服务。 */
    private boolean forwardAuth = true;

    /** @ExchangeClient 接口扫描的基础包。 */
    private List<String> scanBasePackages = List.of("com.zynboot");

    /** 服务地址映射。 */
    private Map<String, String> services = new HashMap<>();

    public String getRequiredUrl(String serviceName) {
        String url = services.get(serviceName);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "Service URL not configured: zyn.exchange.services." + serviceName);
        }
        return url;
    }
}
