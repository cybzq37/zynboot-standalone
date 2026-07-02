package com.zynboot.infra.exchange;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * HttpExchange 客户端构建工具。
 */
public final class ServiceProxyBuilder {

    private ServiceProxyBuilder() {
    }

    public static <T> T build(Class<T> serviceType, String baseUrl,
                              long connectTimeoutMs, long readTimeoutMs,
                              boolean followRedirects, boolean forwardAuth) {
        return build(serviceType, baseUrl, connectTimeoutMs, readTimeoutMs, followRedirects, forwardAuth, List.of());
    }

    public static <T> T build(Class<T> serviceType, String baseUrl,
                              long connectTimeoutMs, long readTimeoutMs,
                              boolean followRedirects, boolean forwardAuth,
                              List<HttpServiceArgumentResolver> argumentResolvers) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(followRedirects
                        ? HttpClient.Redirect.NORMAL
                        : HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient.Builder clientBuilder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);

        if (forwardAuth) {
            clientBuilder.requestInterceptor((request, body, execution) -> {
                var attrs = RequestContextHolder.getRequestAttributes();
                if (attrs instanceof ServletRequestAttributes servletAttrs) {
                    String auth = servletAttrs.getRequest().getHeader("Authorization");
                    if (auth != null && !auth.isBlank()) {
                        request.getHeaders().set("Authorization", auth);
                    }
                }
                return execution.execute(request, body);
            });
        }

        RestClient client = clientBuilder.build();

        var factoryBuilder = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client));

        for (HttpServiceArgumentResolver resolver : argumentResolvers) {
            factoryBuilder.customArgumentResolver(resolver);
        }

        return factoryBuilder.build().createClient(serviceType);
    }
}
