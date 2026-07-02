package com.zynboot.infra.exchange;

import com.zynboot.infra.exchange.query.HttpQueryArgumentResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * FactoryBean：根据 @HttpExchange.url 创建 HTTP 客户端代理。
 * <p>
 * 自动通过 SPI 加载 {@link HttpServiceArgumentResolver} 实现。
 */
@Slf4j
public class ExchangeClientFactoryBean implements FactoryBean<Object>, InitializingBean {

    private final Class<?> clientType;
    private final String serviceName;
    private ExchangeProperties properties;
    private ObjectMapper objectMapper;

    private Object proxy;

    public ExchangeClientFactoryBean(Class<?> clientType, String serviceName) {
        this.clientType = clientType;
        this.serviceName = serviceName;
    }

    public void setProperties(ExchangeProperties properties) {
        this.properties = properties;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties == null) {
            throw new IllegalStateException("ExchangeProperties not injected into " + clientType.getSimpleName());
        }

        String url = properties.getServices().get(serviceName);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "Service URL not configured: zyn.exchange.services." + serviceName);
        }

        List<HttpServiceArgumentResolver> resolvers = new ArrayList<>();
        for (HttpServiceArgumentResolver resolver : ServiceLoader.load(HttpServiceArgumentResolver.class)) {
            if (resolver instanceof HttpQueryArgumentResolver queryResolver && objectMapper != null) {
                queryResolver.setObjectMapper(objectMapper);
            }
            resolvers.add(resolver);
        }

        this.proxy = ServiceProxyBuilder.build(
                clientType, url,
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs(),
                properties.isFollowRedirects(), properties.isForwardAuth(),
                resolvers);
        log.info("Created service client: {} -> {} ({})", clientType.getSimpleName(), serviceName, url);
    }

    @Override
    public Object getObject() {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return clientType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
